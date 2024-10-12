(ns heraldicon.frontend.filter
  (:require
   ["react-infinite-scroll-component" :as InfiniteScroll]
   [clojure.string :as str]
   [heraldicon.avatar :as avatar]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.core :as entity]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.element.search-field :as search-field]
   [heraldicon.frontend.element.select :as select]
   [heraldicon.frontend.element.tags :as tags]
   [heraldicon.frontend.entity.action.favorite :as favorite]
   [heraldicon.frontend.entity.preview :as preview]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.localization.string :as string]
   [heraldicon.static :as static]
   [heraldicon.util.cache :as cache]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(macros/reg-event-db ::filter-toggle-tag
  (fn [db [_ db-path tag]]
    (update-in db db-path (fn [current-tags]
                            (if (get current-tags tag)
                              (dissoc current-tags tag)
                              (assoc current-tags tag true))))))

(defn- normalize-string [s]
  (some-> s
          (.normalize "NFD")))

(defonce normalize-string-for-sort-cache
  (cache/lru-cache 100000))

(defn normalize-string-for-sort [s]
  (let [value (cache/get normalize-string-for-sort-cache s)]
    (if (some? value)
      value
      (let [value (some-> s
                          normalize-string
                          str/lower-case)]
        (cache/put normalize-string-for-sort-cache s value)
        value))))

(defonce normalize-string-for-match-cache
  (cache/lru-cache 100000))

(defn normalize-string-for-match [s]
  (let [value (cache/get normalize-string-for-match-cache s)]
    (if (some? value)
      value
      (let [value (some-> s
                          normalize-string
                          (str/replace #"[\u0300-\u036f]" "")
                          str/lower-case)]
        (cache/put normalize-string-for-match-cache s value)
        value))))

(defn escape-regex [s]
  (let [special-chars (set "\\^$.|?*+()[]{}")]
    (->> s
         (map #(if (special-chars %)
                 (str "\\" %)
                 %))
         (apply str))))

(defonce string-matches?-cache
  (cache/lru-cache 1000000))

(defn- string-matches?
  [s word]
  (let [key [s word]
        value (cache/get string-matches?-cache key)]
    (if (some? value)
      value
      (let [value (cond
                    (and (= (first word) "/")
                         (= (last word) "/")) (try
                                                (re-find (re-pattern (subs word 1 (dec (count word)))) s)
                                                (catch :default _
                                                  nil))

                    (and (= (first word) "\"")
                         (= (last word) "\"")) (let [bounded-regex (re-pattern (str "\\b" (escape-regex (subs word 1 (dec (count word)))) "\\b"))]
                                                 (re-find bounded-regex s))

                    :else (str/includes? s (str/replace word "\"" "")))]
        (cache/put string-matches?-cache key (boolean value))
        value))))

(defonce matches-word-cache
  (cache/lru-cache 1000000))

(defn matches-word? [data word]
  (let [key [data word]
        value (cache/get matches-word-cache key)]
    (if (some? value)
      value
      (let [value (cond
                    (keyword? data) (-> data name (matches-word? word))
                    (string? data) (-> data
                                       normalize-string-for-match
                                       (string-matches? word))
                    (vector? data) (some (fn [e]
                                           (matches-word? e word)) data)
                    (map? data) (some (fn [[k v]]
                                        (or (and (keyword? k)
                                                 (matches-word? k word)
                                                  ;; this would be an attribute entry, the value
                                                  ;; must be truthy as well
                                                 v)
                                            (matches-word? v word))) data))]
        (cache/put matches-word-cache key (boolean value))
        value))))

(defonce split-search-string-cache
  (cache/lru-cache 100000))

(defn split-search-string
  [s]
  (let [value (cache/get split-search-string-cache s)]
    (if (some? value)
      value
      (let [value (re-seq #"/[^/]*/|\"[^\"]+\"|\S+" (normalize-string-for-match s))]
        (cache/put split-search-string-cache s value)
        value))))

(defn- filter-items [session item-list filter-keys filter-string filter-tags filter-access filter-ownership filter-favorites]
  (let [words (split-search-string (or filter-string ""))
        session-username (:username session)
        filter-tags-set (keys filter-tags)]
    (filterv (fn [{:keys [id username access tags]
                   :as item}]
               (and (or (not filter-favorites)
                        @(rf/subscribe [::favorite/is-user-favorite? id]))
                    (case filter-ownership
                      :mine (= username session-username)
                      :heraldicon (= username "heraldicon")
                      :community (not= username "heraldicon")
                      true)
                    (case filter-access
                      :public (= access :public)
                      :private (= access :private)
                      true)
                    (every? (fn [word]
                              (some (fn [attribute]
                                      (-> item
                                          (get-in attribute)
                                          (matches-word? word)))
                                    filter-keys))
                            words)
                    (every? #(get tags %) filter-tags-set)))
             item-list)))

(macros/reg-event-db ::show-more
  (fn [db [_ db-path page-size]]
    (update-in db db-path (fn [value]
                            (+ (or value page-size) page-size)))))

(rf/reg-sub ::filtered-item
  (fn [[_ path _item-id] _]
    (rf/subscribe [:get path]))

  (fn [items [_ _path item-id]]
    (first (filter #(= (:id %) item-id) items))))

(rf/reg-sub ::filtered-item-selected?
  (fn [[_ path _item-id] _]
    (rf/subscribe [:get path]))

  (fn [selected-item [_ _path item-id]]
    (= (:id selected-item) item-id)))

(defn- new-badge []
  [:img.new-badge {:src (static/static-url "/img/new-badge.png")}])

(defn- updated-badge []
  [:img.updated-badge {:src (static/static-url "/img/updated-badge.png")}])

(defn- list-mode [filter-list-mode-path default-list-mode]
  (or @(rf/subscribe [:get filter-list-mode-path])
      default-list-mode
      :normal))

(defn- result-card [items-path item-id kind on-select selected-item-path & {:keys [selection-placeholder?
                                                                                   filter-tags-path
                                                                                   filter-tags
                                                                                   filter-list-mode-path
                                                                                   default-list-mode
                                                                                   title-fn]}]
  (let [{:keys [username]
         :as item} @(rf/subscribe [::filtered-item items-path item-id])
        selected? @(rf/subscribe [::filtered-item-selected? selected-item-path item-id])
        own-username (:username @(rf/subscribe [::session/data]))
        small? (= (list-mode filter-list-mode-path default-list-mode) :small)
        title-fn (or title-fn :name)
        title (title-fn item)]
    [:li.filter-result-card-wrapper
     [:div.filter-result-card {:class (when (and item selected?) "selected")
                               :style (when selection-placeholder?
                                        {:border "1px solid #888"
                                         :border-radius 0})}
      (when-not small?
        [:div.filter-result-card-header
         [:div.filter-result-card-owner
          (when item
            [:a {:href (attribution/full-url-for-username username)
                 :target "_blank"
                 :title username}
             [:img {:src (avatar/url username)
                    :style {:border-radius "50%"}}]])]
         [:div.filter-result-card-title
          {:title title}
          title]
         (when item
           [:div.filter-result-card-access
            (when (= own-username username)
              (if (-> item :access (= :public))
                [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
                [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))])])
      [(if item
         :a.filter-result-card-preview
         :div.filter-result-card-preview) (merge {:title (tr (string/str-tr
                                                              title " " :string.miscellaneous/by " " username))}
                                                 (when on-select
                                                   (on-select item)))
       (if item
         [preview/image kind item]
         [:div.filter-no-item-selected
          [tr :string.miscellaneous/no-item-selected]])
       (when-not small?
         (cond
           (entity/recently-created? item) [new-badge]
           (entity/recently-updated? item) [updated-badge]
           :else nil))]

      (when-not small?
        [:div.filter-result-card-tags
         (when item
           [:<>
            [:div.favorites {:style {:padding-left "10px"}}
             [favorite/button item-id :height 18]]
            [tags/tags-view (-> item :tags keys)
             :on-click #(rf/dispatch [::filter-toggle-tag filter-tags-path %])
             :selected filter-tags
             :style {:display "flex"
                     :flex-flow "row"
                     :flex-wrap "wrap"
                     :width "auto"
                     :overflow "hidden"
                     :height "25px"}]])])]]))

(def ^:private ownership-default
  :all)

(def ^:private access-default
  :all)

(def ^:private sorting-default
  :favorites)

(defn- entity-sort-key-fn [sorting {:keys [name first-version-created-at created-at
                                           type id version]}]
  [(case sorting
     :favorites (- @(rf/subscribe [::favorite/favorite-count id]))
     :creation (- (js/Date.)
                  (js/Date. first-version-created-at))
     :update (- (js/Date.)
                (js/Date. created-at))
     (normalize-string-for-sort name))
   type
   (normalize-string-for-sort name)
   id
   version])

(defn- heraldicon? [{:keys [username]}]
  (= username "heraldicon"))

(defn- results-count [id session items-subscription filter-keys & {:keys [hide-ownership-filter?
                                                                          hide-access-filter?
                                                                          predicate-fn]}]
  (status/default
   items-subscription
   (fn [{all-items :entities}]
     (let [filter-path [:ui :filter id]
           filter-string-path (conj filter-path :filter-string)
           filter-tags-path (conj filter-path :filter-tags)
           filter-access-path (conj filter-path :filter-access)
           filter-string @(rf/subscribe [:get filter-string-path])
           filter-ownership-path (conj filter-path :filter-ownership)
           filter-tags @(rf/subscribe [:get filter-tags-path])
           filter-favorites-path (conj filter-path :filter-favorites)
           filter-favorites @(rf/subscribe [:get filter-favorites-path])
           filter-ownership (if-not hide-ownership-filter?
                              @(rf/subscribe [:get filter-ownership-path])
                              ownership-default)
           consider-filter-access? (and (not hide-access-filter?)
                                        (or (= filter-ownership :mine)
                                            (entity.user/admin? session)))
           filter-access (if consider-filter-access?
                           @(rf/subscribe [:get filter-access-path])
                           access-default)
           all-items (cond->> all-items
                       predicate-fn (filterv predicate-fn))
           filtered-items (filter-items session
                                        all-items
                                        filter-keys
                                        filter-string
                                        filter-tags
                                        filter-access
                                        filter-ownership
                                        filter-favorites)
           total-count (count all-items)
           filtered-count (count filtered-items)]
       [:div {:style {:display "inline"
                      :margin-left "10px"}}
        (if (= filtered-count total-count)
          (str total-count)
          (str filtered-count "/" total-count))
        " "
        [tr (if (= filtered-count 1)
              :string.miscellaneous/item
              :string.miscellaneous/items)]]))
   :on-error (fn [_])
   :on-default (fn [_])))

(defn- results [id session items-subscription filter-keys kind on-select & {:keys [page-size
                                                                                   hide-ownership-filter?
                                                                                   hide-access-filter?
                                                                                   selected-item
                                                                                   predicate-fn
                                                                                   initial-sorting-mode
                                                                                   favour-heraldicon?
                                                                                   display-selected-item?
                                                                                   default-list-mode
                                                                                   title-fn]}]
  (status/default
   items-subscription
   (fn [{all-items-path :path
         all-items :entities}]
     (let [filter-path [:ui :filter id]
           selected-item-path (conj filter-path :selected-item)
           filter-string-path (conj filter-path :filter-string)
           filter-list-mode-path (conj filter-path :list-mode)
           filter-tags-path (conj filter-path :filter-tags)
           filter-access-path (conj filter-path :filter-access)
           filter-sorting-path (conj filter-path :filter-sorting)
           filter-string @(rf/subscribe [:get filter-string-path])
           filter-ownership-path (conj filter-path :filter-ownership)
           filter-favorites-path (conj filter-path :filter-favorites)
           filter-favorites @(rf/subscribe [:get filter-favorites-path])
           filter-tags @(rf/subscribe [:get filter-tags-path])
           filter-ownership (if-not hide-ownership-filter?
                              @(rf/subscribe [:get filter-ownership-path])
                              ownership-default)
           consider-filter-access? (and (not hide-access-filter?)
                                        (or (= filter-ownership :mine)
                                            (entity.user/admin? session)))
           sorting (or @(rf/subscribe [:get filter-sorting-path])
                       initial-sorting-mode
                       sorting-default)
           filter-access (if consider-filter-access?
                           @(rf/subscribe [:get filter-access-path])
                           access-default)
           all-items (cond->> all-items
                       predicate-fn (filterv predicate-fn))
           filtered-items (filter-items session
                                        all-items
                                        filter-keys
                                        filter-string
                                        filter-tags
                                        filter-access
                                        filter-ownership
                                        filter-favorites)
           tags-to-display (frequencies (mapcat (comp keys :tags) filtered-items))
           sorted-items (sort-by (partial entity-sort-key-fn sorting) filtered-items)
           sorted-items (cond->> sorted-items
                          (and favour-heraldicon?
                               (= sorting :name)) (sort-by heraldicon?))
           number-of-items-path [:ui :filter id [filter-keys filter-string filter-tags filter-access filter-ownership]]
           crawler? @(rf/subscribe [:get [:ui :crawler?]])
           small? (= (list-mode filter-list-mode-path default-list-mode) :small)
           page-size (cond-> (or page-size 20)
                       small? (* 5))
           number-of-items (or (when crawler?
                                 (count filtered-items))
                               @(rf/subscribe [:get number-of-items-path])
                               page-size)
           display-items (cond->> sorted-items
                           page-size (take number-of-items))
           results-id (str "filter-results-" id)]
       [:<>
        [:div.filter-component-tags
         [tags/tags-view tags-to-display
          :on-click #(rf/dispatch [::filter-toggle-tag filter-tags-path %])
          :selected filter-tags
          :style {:display "flex"
                  :flex-flow "row"
                  :flex-wrap "wrap"
                  :width "auto"
                  :overflow "hidden"
                  :height "25px"}]]

        [:div.filter-component-results {:id results-id}
         (if (empty? display-items)
           [:div [tr :string.miscellaneous/none]]
           [:> InfiniteScroll
            {:dataLength (count display-items)
             :hasMore (not= (count filtered-items)
                            (count display-items))
             :next #(rf/dispatch [::show-more
                                  number-of-items-path
                                  page-size])
             :scrollableTarget results-id
             :style {:overflow "visible"}}
            [:ul.filter-results {:class (when small? "small")}
             (when display-selected-item?
               [result-card all-items-path (:id selected-item) kind nil selected-item-path
                :filter-tags-path filter-tags-path
                :filter-tags filter-tags
                :selection-placeholder? true
                :filter-list-mode-path filter-list-mode-path
                :default-list-mode default-list-mode
                :title-fn title-fn])
             (into [:<>]
                   (map (fn [item]
                          ^{:key (:id item)}
                          [result-card all-items-path (:id item) kind on-select selected-item-path
                           :filter-tags-path filter-tags-path
                           :filter-tags filter-tags
                           :filter-list-mode-path filter-list-mode-path
                           :default-list-mode default-list-mode
                           :title-fn title-fn]))
                   display-items)
             (when-not (= (count filtered-items)
                          (count display-items))
               [:li.filter-result-card-wrapper.filter-component-show-more
                [:button.button {:on-click #(rf/dispatch [::show-more
                                                          number-of-items-path
                                                          page-size])}
                 [tr :string.miscellaneous/show-more]]])]])]]))))

(defn- list-mode-choice [filter-list-mode-path default-list-mode]
  (let [current-list-mode (list-mode filter-list-mode-path default-list-mode)]
    (into [:div {:style {:display "inline-block"
                         :margin-left "10px"}}]
          (map (fn [[list-mode class]]
                 ^{:key list-mode}
                 [:a {:style {:margin-left "10px"}
                      :href "#"
                      :on-click (js-event/handled #(rf/dispatch [:set filter-list-mode-path list-mode]))}
                  [:i {:class class
                       :style {:color (when (not= current-list-mode list-mode)
                                        "#ccc")}}]]))
          [[:normal "fas fa-th-large"]
           [:small "fas fa-th"]])))

(defn component [id session items-subscription filter-keys kind on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                                               hide-access-filter?
                                                                                               on-filter-string-change
                                                                                               component-styles
                                                                                               initial-sorting-mode
                                                                                               selected-item
                                                                                               default-list-mode]
                                                                                        :as options}]
  (let [filter-path [:ui :filter id]
        selected-item-path (conj filter-path :selected-item)
        filter-string-path (conj filter-path :filter-string)
        filter-list-mode-path (conj filter-path :list-mode)
        filter-access-path (conj filter-path :filter-access)
        filter-sorting-path (conj filter-path :filter-sorting)
        filter-ownership-path (conj filter-path :filter-ownership)
        filter-favorites-path (conj filter-path :filter-favorites)
        filter-ownership (if-not hide-ownership-filter?
                           @(rf/subscribe [:get filter-ownership-path])
                           ownership-default)
        logged-in? (:username session)
        consider-filter-access? (and (not hide-access-filter?)
                                     logged-in?
                                     (or (= filter-ownership :mine)
                                         (entity.user/admin? session)))
        stored-selected-item @(rf/subscribe [:get selected-item-path])]
    (when (not= stored-selected-item selected-item)
      (rf/dispatch [:set selected-item-path selected-item]))

    [:div.filter-component {:style component-styles}
     [:div.filter-component-search
      [search-field/search-field {:path filter-string-path}
       :on-change (fn [value]
                    (rf/dispatch [:set filter-string-path value])
                    (when on-filter-string-change
                      (on-filter-string-change)))]
      (when refresh-fn
        [:a {:style {:margin-left "10px"}
             :on-click #(do
                          (refresh-fn)
                          (.stopPropagation %))} [:i.fas.fa-sync-alt]])
      [list-mode-choice filter-list-mode-path default-list-mode]

      (when logged-in?
        (let [on? @(rf/subscribe [:get filter-favorites-path])]
          [:div {:on-click #(rf/dispatch [:set filter-favorites-path (not on?)])
                 :title (tr :string.option/favorites-filter)
                 :style {:display "inline-block"
                         :margin-left "10px"
                         :cursor "pointer"}}
           [favorite/icon 20 on?]]))

      (when (and (not hide-ownership-filter?)
                 logged-in?)
        [select/raw-select-inline
         {:path filter-ownership-path}
         @(rf/subscribe [:get {:path filter-ownership-path}])
         (cond-> [[:string.option.ownership-filter-choice/all :all]
                  [:string.option.ownership-filter-choice/mine :mine]]
           (#{:charge :ribbon} kind) (concat [[:string.option.ownership-filter-choice/heraldicon :heraldicon]
                                              [:string.option.ownership-filter-choice/community :community]]))
         :value-prefix :string.option/show
         :style {:margin-left "10px"
                 :margin-bottom "5px"}])

      (when consider-filter-access?
        [select/raw-select-inline
         {:path filter-access-path}
         @(rf/subscribe [:get {:path filter-access-path}])
         [[:string.option.access-filter-choice/all :all]
          [:string.option.access-filter-choice/public :public]
          [:string.option.access-filter-choice/private :private]]
         :value-prefix :string.option/access
         :style {:margin-left "10px"
                 :margin-bottom "5px"}])

      [select/raw-select-inline {:path filter-sorting-path}
       (or @(rf/subscribe [:get {:path filter-sorting-path}])
           initial-sorting-mode
           sorting-default)
       [[:string.option.sorting-filter-choice/name :name]
        [:string.option.sorting-filter-choice/favorites :favorites]
        [:string.option.sorting-filter-choice/creation :creation]
        [:string.option.sorting-filter-choice/update :update]]
       :value-prefix :string.option/sort-by
       :style {:margin-left "10px"
               :margin-bottom "5px"}]

      [results-count id session items-subscription filter-keys options]]

     [results id session items-subscription filter-keys kind on-select options]]))

#_(defonce timer
    (js/setInterval (fn []
                      (log/info :normalize-string-for-sort-cache (count @(:map normalize-string-for-sort-cache))
                                :normalize-string-for-match-cache (count @(:map normalize-string-for-match-cache))
                                :string-matches?-cache (count @(:map string-matches?-cache))
                                :matches-word-cache (count @(:map matches-word-cache))
                                :split-search-string-cache (count @(:map split-search-string-cache))))
                    60000))
