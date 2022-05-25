(ns heraldicon.frontend.filter
  (:require
   ["react-infinite-scroll-component" :as InfiniteScroll]
   [clojure.set :as set]
   [clojure.string :as s]
   [heraldicon.avatar :as avatar]
   [heraldicon.config :as config]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.preview :as preview]
   [heraldicon.frontend.ui.element.checkbox :as checkbox]
   [heraldicon.frontend.ui.element.radio-select :as radio-select]
   [heraldicon.frontend.ui.element.search-field :as search-field]
   [heraldicon.frontend.ui.element.tags :as tags]
   [heraldicon.frontend.user :as user]
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

(defn normalize-string-for-sort [s]
  (some-> s
          normalize-string
          s/lower-case))

(defn normalize-string-for-match [s]
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

(defn filter-items [user-data item-list filter-keys filter-string filter-tags filter-access filter-ownership]
  (let [words (-> filter-string
                  normalize-string-for-match
                  (s/split #" +"))
        filter-tags-set (-> filter-tags
                            keys
                            set)]
    (filterv (fn [item]
               (and (case filter-ownership
                      :mine (= (:username item)
                               (:username user-data))
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
    (->> items
         (filter #(-> % :id (= item-id)))
         first)))

(rf/reg-sub ::filtered-item-selected?
  (fn [[_ path _item-id] _]
    (rf/subscribe [:get path]))

  (fn [selected-item [_ _path item-id]]
    (-> selected-item :id (= item-id))))

(defn heraldicon-tag []
  [:span.tag {:style {:background "#3e933f"
                      :color "#f6f6f6"}}
   "heraldicon"])

(defn community-tag []
  [:span.tag {:style {:background "#bf7433"
                      :color "#f6f6f6"}}
   "community"])

(defn result-card [items-path item-id kind on-select selected-item-path & {:keys [selection-placeholder?]}]
  (let [item @(rf/subscribe [::filtered-item items-path item-id])
        selected? @(rf/subscribe [::filtered-item-selected? selected-item-path item-id])
        username (:username item)
        own-username (:username (user/data))]
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
        (:name item)]
       (when item
         [:div.filter-result-card-access
          (when (= own-username username)
            (if (-> item :access (= :public))
              [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
              [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))])]
      [(if item
         :a.filter-result-card-preview
         :div.filter-result-card-preview) (when on-select
                                            (on-select item))
       (if item
         [preview/preview-image kind item]
         [:div.filter-no-item-selected
          [tr :string.miscellaneous/no-item-selected]])]
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

(defn component [id user-data all-items-path filter-keys kind on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                                             hide-access-filter?
                                                                                             on-filter-string-change
                                                                                             component-styles
                                                                                             page-size
                                                                                             sort-fn
                                                                                             selected-item
                                                                                             display-selected-item?]}]
  (let [filter-path [:ui :filter id]
        selected-item-path (conj filter-path :selected-item)
        filter-string-path (conj filter-path :filter-string)
        filter-tags-path (conj filter-path :filter-tags)
        filter-access-path (conj filter-path :filter-access)
        filter-ownership-path (conj filter-path :filter-ownership)
        filter-string @(rf/subscribe [:get filter-string-path])
        filter-tags @(rf/subscribe [:get filter-tags-path])
        filter-ownership (if-not hide-ownership-filter?
                           @(rf/subscribe [:get filter-ownership-path])
                           :all)
        consider-filter-access? (and (not hide-access-filter?)
                                     (or (= filter-ownership :mine)
                                         (-> user-data :username ((config/get :admins)))))
        filter-access (if consider-filter-access?
                        @(rf/subscribe [:get filter-access-path])
                        :all)
        all-items @(rf/subscribe [:get all-items-path])
        filtered-items (filter-items user-data
                                     all-items
                                     filter-keys
                                     filter-string
                                     filter-tags
                                     filter-access
                                     filter-ownership)
        tags-to-display (frequencies (mapcat (comp keys :tags) filtered-items))
        sorted-items (sort-by (fn [item]
                                ;; put heraldicon items in front
                                [(if (-> item :username (= "heraldicon"))
                                   0
                                   1)
                                 (when sort-fn
                                   (sort-fn item))]) filtered-items)
        number-of-items-path [:ui :filter id [filter-keys filter-string filter-tags filter-access filter-ownership]]
        list-all? @(rf/subscribe [:get [:ui :list-all?]])
        number-of-items (or (when list-all?
                              (count filtered-items))
                            @(rf/subscribe [:get number-of-items-path])
                            page-size)
        display-items (cond->> sorted-items
                        page-size (take number-of-items))
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
        [:div {:style {:border-right (when consider-filter-access? "1px solid #888")}}
         [radio-select/radio-select {:path filter-ownership-path}
          :option {:type :choice
                   :default :all
                   :choices (concat [[:string.option.ownership-filter-choice/all :all]
                                     [:string.option.ownership-filter-choice/mine :mine]]
                                    (when (#{:charge :ribbon} kind)
                                      [[:string.option.ownership-filter-choice/heraldicon :heraldicon]
                                       [:string.option.ownership-filter-choice/community :community]]))}]])

      (when consider-filter-access?
        [radio-select/radio-select {:path filter-access-path}
         :option {:type :choice
                  :default :all
                  :choices [[:string.option.access-filter-choice/all :all]
                            [:string.option.access-filter-choice/public :public]
                            [:string.option.access-filter-choice/private :private]]}])]

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
                [tr :string.miscellaneous/show-more]]])]])])]))

(defn legacy-component [id user-data all-items filter-keys display-fn refresh-fn & {:keys [hide-ownership-filter?
                                                                                           hide-access-filter?
                                                                                           on-filter-string-change
                                                                                           component-styles
                                                                                           page-size
                                                                                           sort-fn]}]
  (let [filter-path [:ui :filter id]
        filter-string-path (conj filter-path :filter-string)
        filter-tags-path (conj filter-path :filter-tags)
        filter-access-path (conj filter-path :filter-access)
        filter-ownership-path (conj filter-path :filter-ownership)
        filter-string @(rf/subscribe [:get filter-string-path])
        filter-tags @(rf/subscribe [:get filter-tags-path])
        filter-access @(rf/subscribe [:get filter-access-path])
        filter-ownership (if @(rf/subscribe [:get filter-ownership-path])
                           :mine
                           :all)
        filtered-items (filter-items user-data
                                     all-items
                                     filter-keys
                                     filter-string
                                     filter-tags
                                     filter-access
                                     filter-ownership)
        tags-to-display (frequencies (mapcat (comp keys :tags) filtered-items))
        filtered? (or (-> filter-string count pos?)
                      (-> filter-tags count pos?))
        sorted-items (cond->> filtered-items
                       sort-fn (sort-by sort-fn))
        number-of-items-path [:ui :filter id [filter-keys filter-string filter-tags filter-access filter-ownership]]
        number-of-items (or @(rf/subscribe [:get number-of-items-path])
                            page-size)
        display-items (cond->> sorted-items
                        page-size (take number-of-items))]
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
        [checkbox/checkbox {:path filter-ownership-path}
         :option {:type :boolean
                  :ui {:label :string.miscellaneous/mine-only}}])
      (when-not hide-access-filter?
        [radio-select/radio-select {:path filter-access-path}
         :option {:type :choice
                  :default :all
                  :choices [[:string.option.access-filter-choice/all :all]
                            [:string.option.access-filter-choice/public :public]
                            [:string.option.access-filter-choice/private :private]]}])]

     [:div.filter-component-tags
      [tags/tags-view tags-to-display
       :on-click #(rf/dispatch [::filter-toggle-tag filter-tags-path %])
       :selected filter-tags]]

     [:div.filter-component-results
      (if (empty? display-items)
        [:div [tr :string.miscellaneous/none]]
        [display-fn
         :items display-items
         :filtered? filtered?])]
     (when-not (= (count filtered-items)
                  (count display-items))
       [:div.filter-component-show-more
        [:button.button {:on-click #(rf/dispatch [::show-more
                                                  number-of-items-path
                                                  page-size])}
         [tr :string.miscellaneous/show-more]]])]))
