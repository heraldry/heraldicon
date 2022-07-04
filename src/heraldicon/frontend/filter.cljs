(ns heraldicon.frontend.filter
  (:require
   ["react-infinite-scroll-component" :as InfiniteScroll]
   [clojure.set :as set]
   [clojure.string :as s]
   [heraldicon.avatar :as avatar]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.core :as entity]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.element.radio-select :as radio-select]
   [heraldicon.frontend.element.search-field :as search-field]
   [heraldicon.frontend.element.tags :as tags]
   [heraldicon.frontend.entity.preview :as preview]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(macros/reg-event-db ::filter-toggle-tag
  (fn [db [_ db-path tag]]
    (update-in db db-path (fn [current-tags]
                            (if (get current-tags tag)
                              (dissoc current-tags tag)
                              (assoc current-tags tag true))))))

(defn- normalize-string [s]
  (some-> s
          (.normalize "NFD")))

(defn- normalize-string-for-sort [s]
  (some-> s
          normalize-string
          s/lower-case))

(defn- normalize-string-for-match [s]
  (some-> s
          normalize-string
          (s/replace #"[\u0300-\u036f]" "")
          s/lower-case))

(defn- matches-word [data word]
  (cond
    (keyword? data) (-> data name (matches-word word))
    (string? data) (-> data normalize-string-for-match
                       (s/includes? word))
    (vector? data) (some (fn [e]
                           (matches-word e word)) data)
    (map? data) (some (fn [[k v]]
                        (or (and (keyword? k)
                                 (matches-word k word)
                                 ;; this would be an attribute entry, the value
                                 ;; must be truthy as well
                                 v)
                            (matches-word v word))) data)))

(defn- filter-items [session item-list filter-keys filter-string filter-tags filter-access filter-ownership]
  (let [words (-> filter-string
                  normalize-string-for-match
                  (s/split #" +"))
        filter-tags-set (-> filter-tags
                            keys
                            set)]
    (filterv (fn [item]
               (and (case filter-ownership
                      :mine (= (:username item)
                               (:username session))
                      :heraldicon (= (:username item) "heraldicon")
                      :community (not= (:username item) "heraldicon")
                      true)
                    (if (#{:public :private} filter-access)
                      (-> item :access (= filter-access))
                      true)
                    (every? (fn [word]
                              (some (fn [attribute]
                                      (-> item
                                          ((if (seqable? attribute)
                                             get-in
                                             get) attribute)
                                          (matches-word word)))
                                    filter-keys))
                            words)
                    (set/subset? filter-tags-set
                                 (-> item
                                     :tags
                                     keys
                                     set))))
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

(defn- heraldicon-tag []
  [:span.tag {:style {:background "#3e933f"
                      :color "#f6f6f6"}}
   "heraldicon"])

(defn- community-tag []
  [:span.tag {:style {:background "#bf7433"
                      :color "#f6f6f6"}}
   "community"])

(defn- new-badge []
  [:img.new-badge {:src (static/static-url "/img/new-badge.png")}])

(defn- updated-badge []
  [:img.updated-badge {:src (static/static-url "/img/updated-badge.png")}])

(defn- result-card [items-path item-id kind on-select selected-item-path & {:keys [selection-placeholder?]}]
  (let [{:keys [username]
         :as item} @(rf/subscribe [::filtered-item items-path item-id])
        selected? @(rf/subscribe [::filtered-item-selected? selected-item-path item-id])
        own-username (:username @(rf/subscribe [::session/data]))]
    [:li.filter-result-card-wrapper
     [:div.filter-result-card {:class (when (and item selected?) "selected")
                               :style (when selection-placeholder?
                                        {:border "1px solid #888"
                                         :border-radius 0})}
      [:div.filter-result-card-header
       [:div.filter-result-card-owner
        (when item
          [:a {:href (attribution/full-url-for-username username)
               :target "_blank"
               :title username}
           [:img {:src (avatar/url username)
                  :style {:border-radius "50%"}}]])]
       [:div.filter-result-card-title
        {:title (:name item)}
        (:name item)]
       (when item
         [:div.filter-result-card-access
          (when (= own-username username)
            (if (-> item :access (= :public))
              [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
              [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))])]
      [(if item
         :a.filter-result-card-preview
         :div.filter-result-card-preview) (merge {:title (:name item)}
                                                 (when on-select
                                                   (on-select item)))
       (if item
         [preview/image kind item]
         [:div.filter-no-item-selected
          [tr :string.miscellaneous/no-item-selected]])
       (cond
         (entity/recently-created? item) [new-badge]
         (entity/recently-updated? item) [updated-badge]
         :else nil)]
      [:div.filter-result-card-tags
       (when item
         [:<>
          [:div.item-classification {:style {:padding-left "10px"}}
           (if (= username "heraldicon")
             [heraldicon-tag]
             [community-tag])]
          [tags/tags-view (-> item :tags keys)
           :style {:display "flex"
                   :flex-flow "row"
                   :flex-wrap "wrap"
                   :width "auto"
                   :overflow "hidden"
                   :height "25px"}]])]]]))

(def ^:private ownership-default
  :all)

(def ^:private access-default
  :all)

(def ^:private sorting-default
  :creation)

(defn- entity-sort-key-fn [sorting {:keys [name first-version-created-at created-at
                                           type id version]}]
  ;; put heraldicon items in front
  [(case sorting
     :creation (- (js/Date.)
                  (js/Date. first-version-created-at))
     :update (- (js/Date.)
                (js/Date. created-at))
     (normalize-string-for-sort name))
   type
   id
   version])

(defn- heraldicon? [{:keys [username]}]
  (= username "heraldicon"))

(defn- results [id session items-subscription filter-keys kind on-select & {:keys [page-size
                                                                                   hide-ownership-filter?
                                                                                   hide-access-filter?
                                                                                   selected-item
                                                                                   predicate-fn
                                                                                   initial-sorting-mode
                                                                                   favour-heraldicon?
                                                                                   display-selected-item?]}]
  (status/default
   items-subscription
   (fn [{all-items-path :path
         all-items :entities}]
     (let [filter-path [:ui :filter id]
           selected-item-path (conj filter-path :selected-item)
           filter-string-path (conj filter-path :filter-string)
           filter-tags-path (conj filter-path :filter-tags)
           filter-access-path (conj filter-path :filter-access)
           filter-sorting-path (conj filter-path :filter-sorting)
           filter-string @(rf/subscribe [:get filter-string-path])
           filter-ownership-path (conj filter-path :filter-ownership)
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
                                        filter-ownership)
           tags-to-display (frequencies (mapcat (comp keys :tags) filtered-items))
           sorted-items (sort-by (partial entity-sort-key-fn sorting) filtered-items)
           sorted-items (cond->> sorted-items
                          (and favour-heraldicon?
                               (= sorting :name)) (sort-by heraldicon?))
           number-of-items-path [:ui :filter id [filter-keys filter-string filter-tags filter-access filter-ownership]]
           crawler? @(rf/subscribe [:get [:ui :crawler?]])
           number-of-items (or (when crawler?
                                 (count filtered-items))
                               @(rf/subscribe [:get number-of-items-path])
                               page-size)
           display-items (cond->> sorted-items
                           page-size (take number-of-items))]
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

        (let [results-id (str "filter-results-" id)]
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
              [:ul.filter-results
               (when display-selected-item?
                 [result-card all-items-path (:id selected-item) kind nil selected-item-path
                  :selection-placeholder? true])
               (into [:<>]
                     (map (fn [item]
                            ^{:key (:id item)}
                            [result-card all-items-path (:id item) kind on-select selected-item-path]))
                     display-items)
               (when-not (= (count filtered-items)
                            (count display-items))
                 [:li.filter-result-card-wrapper.filter-component-show-more
                  [:button.button {:on-click #(rf/dispatch [::show-more
                                                            number-of-items-path
                                                            page-size])}
                   [tr :string.miscellaneous/show-more]]])]])])]))))

(defn component [id session items-subscription filter-keys kind on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                                               hide-access-filter?
                                                                                               on-filter-string-change
                                                                                               component-styles
                                                                                               initial-sorting-mode
                                                                                               selected-item]
                                                                                        :as options}]
  (let [filter-path [:ui :filter id]
        selected-item-path (conj filter-path :selected-item)
        filter-string-path (conj filter-path :filter-string)
        filter-access-path (conj filter-path :filter-access)
        filter-sorting-path (conj filter-path :filter-sorting)
        filter-ownership-path (conj filter-path :filter-ownership)
        filter-ownership (if-not hide-ownership-filter?
                           @(rf/subscribe [:get filter-ownership-path])
                           ownership-default)
        consider-filter-access? (and (not hide-access-filter?)
                                     (or (= filter-ownership :mine)
                                         (entity.user/admin? session)))
        stored-selected-item @(rf/subscribe [:get selected-item-path])]

    (when (not= stored-selected-item selected-item)
      (rf/dispatch [:set selected-item-path selected-item]))

    [:div.filter-component {:style component-styles}
     [:div.filter-component-search
      [search-field/search-field {:path filter-string-path}
       :on-change (fn [value]
                    (rf/dispatch-sync [:set filter-string-path value])
                    (when on-filter-string-change
                      (on-filter-string-change)))]
      (when refresh-fn
        [:a {:style {:margin-left "0.5em"}
             :on-click #(do
                          (refresh-fn)
                          (.stopPropagation %))} [:i.fas.fa-sync-alt]])]
     [:div.filter-component-filters
      (when-not hide-ownership-filter?
        [radio-select/radio-select {:path filter-ownership-path}
         :option {:type :option.type/choice
                  :default ownership-default
                  :choices (concat [[:string.option.ownership-filter-choice/all :all]
                                    [:string.option.ownership-filter-choice/mine :mine]]
                                   (when (#{:charge :ribbon} kind)
                                     [[:string.option.ownership-filter-choice/heraldicon :heraldicon]
                                      [:string.option.ownership-filter-choice/community :community]]))}])

      (when consider-filter-access?
        [radio-select/radio-select {:path filter-access-path}
         :option {:type :option.type/choice
                  :default access-default
                  :choices [[:string.option.access-filter-choice/all :all]
                            [:string.option.access-filter-choice/public :public]
                            [:string.option.access-filter-choice/private :private]]}])

      [:div
       [tr :string.option/sort-by] ":" [radio-select/radio-select {:path filter-sorting-path}
                                        :option {:type :option.type/choice
                                                 :default (or initial-sorting-mode
                                                              sorting-default)
                                                 :choices [[:string.option.sorting-filter-choice/name :name]
                                                           [:string.option.sorting-filter-choice/creation :creation]
                                                           [:string.option.sorting-filter-choice/update :update]]}
                                        :style {:display "inline-block"
                                                :margin-left "0.5em"}]]]

     [results id session items-subscription filter-keys kind on-select options]]))
