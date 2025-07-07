(ns heraldicon.frontend.search-filter
  (:require
   ["react-infinite-scroll-component" :as InfiniteScroll]
   [heraldicon.avatar :as avatar]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.core :as entity]
   [heraldicon.frontend.element.select :as select]
   [heraldicon.frontend.element.tags :as tags]
   [heraldicon.frontend.entity.action.favorite :as favorite]
   [heraldicon.frontend.entity.preview :as preview]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.repository.entity-search :as entity-search]
   [heraldicon.frontend.search-string :as search-string]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.localization.string :as string]
   [heraldicon.static :as static]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private default-ownership
  :all)

(def ^:private default-access
  :all)

(def ^:private default-sorting
  :favorites)

(def ^:private default-list-mode
  :normal)

(def ^:private default-favorites?
  false)

(defn- filter-temporary-search-string-path [id]
  [:ui :filter id :filter-temporary-search-string])

(defn- filter-search-string-path [id]
  [:ui :filter id :filter-search-string])

(defn- filter-list-mode-path [id]
  [:ui :filter id :filter-list-mode])

(defn- filter-favorites?-path [id]
  [:ui :filter id :filter-favorites?])

(defn- filter-ownership-path [id]
  [:ui :filter id :filter-ownership])

(defn- filter-access-path [id]
  [:ui :filter id :filter-access])

(defn- filter-sorting-path [id]
  [:ui :filter id :filter-sorting])

(defn- filter-tags-path [id]
  [:ui :filter id :filter-tags])

(macros/reg-event-db ::filter-toggle-tag
  (fn [db [_ id tag]]
    (update-in db (filter-tags-path id) (fn [current-tags]
                                          (if (get current-tags tag)
                                            (dissoc current-tags tag)
                                            (assoc current-tags tag true))))))

(rf/reg-sub ::search-result-item
  (fn [[_ id entity-type _item-id] _]
    (rf/subscribe [::entity-search/data-raw id entity-type]))

  (fn [items [_ _id _entity-type item-id]]
    (first (filter #(= (:id %) item-id) items))))

(defn- new-badge []
  [:img.new-badge {:src (static/static-url "/img/new-badge.png")}])

(defn- updated-badge []
  [:img.updated-badge {:src (static/static-url "/img/updated-badge.png")}])

(defn- get-list-mode [id options]
  (let [list-mode-path (filter-list-mode-path id)]
    (or @(rf/subscribe [:get list-mode-path])
        (:default-list-mode options)
        default-list-mode)))

(defn- get-access [id]
  (or @(rf/subscribe [:get {:path (filter-access-path id)}])
      default-access))

(defn- get-ownership [id {:keys [hide-ownership-filter?]}]
  (if-not hide-ownership-filter?
    @(rf/subscribe [:get (filter-ownership-path id)])
    default-ownership))

(defn- get-sorting [id {:keys [initial-sorting-mode]}]
  (or @(rf/subscribe [:get {:path (filter-sorting-path id)}])
      initial-sorting-mode
      default-sorting))

(defn- get-favorites? [id]
  (or @(rf/subscribe [:get (filter-favorites?-path id)])
      default-favorites?))

(defn- get-search-string [id]
  (or @(rf/subscribe [:get (filter-search-string-path id)])
      ""))

(defn- get-tags [id]
  (into []
        (keep
         (fn [[key value]]
           (when value
             key)))
        @(rf/subscribe [:get (filter-tags-path id)])))

(defn- result-card [id item-id kind on-select {:keys [selection-placeholder?
                                                      selected-item
                                                      filter-tags
                                                      title-fn]
                                               :as options}]
  (let [{:keys [username]
         :as item} (if (= (:id selected-item) item-id)
                     selected-item
                     @(rf/subscribe [::search-result-item id kind item-id]))
        selected? false
        own-username (:username @(rf/subscribe [::session/data]))
        small? (= (get-list-mode id options) :small)
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
             :on-click #(rf/dispatch [::filter-toggle-tag id %])
             :selected filter-tags
             :style {:display "flex"
                     :flex-flow "row"
                     :flex-wrap "wrap"
                     :width "auto"
                     :overflow "hidden"
                     :height "25px"}]])])]]))

(defn- prepare-query [id {:keys [filter-username]
                          :as options}]
  {:phrases (search-string/split (get-search-string id))
   :access (get-access id)
   :username (or (when (= (get-ownership id options) :mine)
                   (:username @(rf/subscribe [::session/data])))
                 filter-username)
   :tags (get-tags id)
   :favorites? (get-favorites? id)
   :sort (get-sorting id options)
   :page-size (or (get options :page-size)
                  25)})

(defn- get-items-subscription [id kind options]
  (rf/subscribe [::entity-search/data id kind (prepare-query id options)]))

(macros/reg-event-db ::copy-search-string-to-query
  (fn [db [_ id]]
    (let [search-value (get-in db (filter-temporary-search-string-path id))]
      (assoc-in db (filter-search-string-path id) search-value))))

(defn- results-count [id kind options]
  (let [items-subscription (get-items-subscription id kind options)]
    [status/default
     items-subscription
     (fn [{:keys [total entities]}]
       [:div {:style {:display "inline"
                      :margin-left "10px"}}
        (count entities) " / "
        total
        " "
        [tr (if (= total 1)
              :string.miscellaneous/item
              :string.miscellaneous/items)]])
     :on-error (fn [_])
     :on-default (fn [_])]))

(defn- results [id kind on-select {:keys [page-size
                                          display-selected-item?
                                          selected-item]
                                   :as options}]
  (let [items-subscription (get-items-subscription id kind options)]
    [status/default
     items-subscription
     (fn [{:keys [entities total tags]}]
       (let [filter-tags @(rf/subscribe [:get (filter-tags-path id)])
             small? (= (get-list-mode id options) :small)
             page-size (cond-> (or page-size 20)
                         small? (* 5))
             results-id (str "filter-results-" id)]
         [:<>
          [:div.filter-component-tags
           [tags/tags-view tags
            :on-click #(rf/dispatch [::filter-toggle-tag id %])
            :selected filter-tags
            :style {:display "flex"
                    :flex-flow "row"
                    :flex-wrap "wrap"
                    :width "auto"
                    :overflow "hidden"
                    :height "25px"}]]

          [:div.filter-component-results {:id results-id}
           (if (empty? entities)
             [:div [tr :string.miscellaneous/none]]
             [:> InfiniteScroll
              {:dataLength (count entities)
               :hasMore (not= (count entities) total)
               :next #(rf/dispatch [::entity-search/load-more id kind page-size])
               :scrollableTarget results-id
               :style {:overflow "visible"}}
              [:ul.filter-results {:class (when small? "small")}
               (when display-selected-item?
                 [result-card id (:id selected-item) kind nil
                  (assoc options
                         :selection-placeholder? true)])
               (into [:<>]
                     (map (fn [item]
                            ^{:key (:id item)}
                            [result-card id (:id item) kind on-select options]))
                     entities)
               (when-not (= (count entities) total)
                 [:li.filter-result-card-wrapper.filter-component-show-more
                  [:button.button {:on-click #(rf/dispatch [::entity-search/load-more id kind page-size])}
                   [tr :string.miscellaneous/show-more]]])]])]]))]))

(defn- list-mode [id options]
  (let [current-list-mode (get-list-mode id options)]
    (into [:div {:style {:display "inline-block"
                         :margin-left "10px"}}]
          (map (fn [[list-mode class]]
                 ^{:key list-mode}
                 [:a {:style {:margin-left "10px"}
                      :href "#"
                      :on-click (js-event/handled #(rf/dispatch [:set (filter-list-mode-path id) list-mode]))}
                  [:i {:class class
                       :style {:color (when (not= current-list-mode list-mode)
                                        "#ccc")}}]]))
          [[:normal "fas fa-th-large"]
           [:small "fas fa-th"]])))

(defn- search-input [id _options]
  (let [path (filter-temporary-search-string-path id)
        value @(rf/subscribe [:get path])
        tmp-value (r/atom value)]
    (fn [id _options]
      [:div.search-field
       [:i.fas.fa-search]
       [:input {:name "search"
                :type "search"
                :value @tmp-value
                :autoComplete "off"
                :on-blur (fn [_event]
                           (rf/dispatch-sync [::copy-search-string-to-query id]))
                :on-key-press (fn [event]
                                (when (-> event .-code (= "Enter"))
                                  (rf/dispatch-sync [::copy-search-string-to-query id])))
                :on-change #(let [value (-> % .-target .-value)]
                              (reset! tmp-value value)
                              (rf/dispatch-sync [:set path @tmp-value]))
                :style {:outline "none"
                        :border "0"
                        :margin-left "0.5em"
                        :width "calc(100% - 12px - 1.5em)"}}]])))

(defn- favorites? [id _options]
  (when @(rf/subscribe [::session/logged-in?])
    (let [on? (get-favorites? id)]
      [:div {:on-click #(rf/dispatch [:set (filter-favorites?-path id) (not on?)])
             :title (tr :string.option/favorites-filter)
             :style {:display "inline-block"
                     :margin-left "10px"
                     :cursor "pointer"}}
       [favorite/icon 20 on?]])))

(defn- ownership [id {:keys [hide-ownership-filter?]
                      :as options}]
  (when (and (not hide-ownership-filter?)
             @(rf/subscribe [::session/logged-in?]))
    [select/raw-select-inline
     {:path (filter-ownership-path id)}
     (get-ownership id options)
     [[:string.option.ownership-filter-choice/all :all]
      [:string.option.ownership-filter-choice/mine :mine]]
     :value-prefix :string.option/show
     :style {:margin-left "10px"
             :margin-bottom "5px"}]))

(defn- access [id {:keys [hide-access-filter?]
                   :as options}]
  (let [consider-filter-access? (and (not hide-access-filter?)
                                     @(rf/subscribe [::session/logged-in?])
                                     (or (= (get-ownership id options) :mine)
                                         @(rf/subscribe [::session/admin?])))]
    (when consider-filter-access?
      [select/raw-select-inline
       {:path (filter-access-path id)}
       (get-access id)
       [[:string.option.access-filter-choice/all :all]
        [:string.option.access-filter-choice/public :public]
        [:string.option.access-filter-choice/private :private]]
       :value-prefix :string.option/access
       :style {:margin-left "10px"
               :margin-bottom "5px"}])))

(defn- sorting [id options]
  [select/raw-select-inline {:path (filter-sorting-path id)}
   (get-sorting id options)
   [[:string.option.sorting-filter-choice/favorites :favorites]
    [:string.option.sorting-filter-choice/creation :created_at]
    [:string.option.sorting-filter-choice/update :modified_at]]
   :value-prefix :string.option/sort-by
   :style {:margin-left "10px"
           :margin-bottom "5px"}])

(defn component [id kind on-select {:keys [component-styles]
                                    :as options}]
  [:div.filter-component {:style component-styles}
   [:div.filter-component-search
    [search-input id options]

    [:button.button.primary
     {:on-click #(rf/dispatch [::copy-search-string-to-query id])
      :style {:margin-left "10px"}}
     "search"]

    [list-mode id options]

    [favorites? id options]

    [ownership id options]

    [access id options]

    [sorting id options]

    [results-count id kind options]]

   [results id kind on-select options]])
