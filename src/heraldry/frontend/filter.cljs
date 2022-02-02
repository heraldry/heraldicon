(ns heraldry.frontend.filter
  (:require
   [clojure.set :as set]
   [clojure.string :as s]
   [heraldry.attribution :as attribution]
   [heraldry.config :as config]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.preview :as preview]
   [heraldry.frontend.ui.element.checkbox :as checkbox]
   [heraldry.frontend.ui.element.radio-select :as radio-select]
   [heraldry.frontend.ui.element.search-field :as search-field]
   [heraldry.frontend.ui.element.tags :as tags]
   [heraldry.frontend.user :as user]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(macros/reg-event-db ::filter-toggle-tag
  (fn [db [_ db-path tag]]
    (update-in db db-path (fn [current-tags]
                            (if (get current-tags tag)
                              (dissoc current-tags tag)
                              (assoc current-tags tag true))))))

(defn selected-item? [selected-item item]
  (= (:id selected-item)
     (:id item)))

(defn filter-items [user-data item-list filter-keys filter-string filter-tags filter-access filter-ownership selected-item]
  (let [words (-> filter-string
                  (s/split #" +")
                  (->> (map s/lower-case)))
        filter-tags-set (-> filter-tags
                            keys
                            set)]
    (filterv (fn [item]
               (or (and selected-item
                        (selected-item? selected-item item))
                   (and (case filter-ownership
                          :mine (= (:username item)
                                   (:username user-data))
                          :heraldicon (= (:username item) "heraldicon")
                          :community (not= (:username item) "heraldicon")
                          true)
                        (case filter-access
                          :public (:is-public item)
                          :private (not (:is-public item))
                          true)
                        (every? (fn [word]
                                  (some (fn [attribute]
                                          (-> item
                                              (get attribute)
                                              (util/matches-word word)))
                                        filter-keys))
                                words)
                        (set/subset? filter-tags-set
                                     (-> item
                                         :tags
                                         keys
                                         set)))))
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

(defn result-card [items-path item-id kind on-select selected-item-path]
  (let [item @(rf/subscribe [::filtered-item items-path item-id])
        selected? @(rf/subscribe [::filtered-item-selected? selected-item-path item-id])
        username (:username item)
        own-username (:username (user/data))]
    [:li.filter-result-card-wrapper
     [:div.filter-result-card {:class (when selected? "selected")}
      [:div.filter-result-card-header
       [:div.filter-result-card-owner
        [:a {:href (attribution/full-url-for-username username)
             :target "_blank"
             :title username}
         [:img {:src (util/avatar-url username)
                :style {:border-radius "50%"}}]]]
       [:div.filter-result-card-title
        (:name item)]
       [:div.filter-result-card-access
        (when (= own-username username)
          (if (:is-public item)
            [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
            [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))]]
      [:div.filter-result-card-preview {:on-click #(on-select item)}
       [preview/preview-image kind item]]
      [:div.filter-result-card-tags
       (when (= kind :charge)
         [:div.item-classification
          (if (= username "heraldicon")
            [heraldicon-tag]
            [community-tag])])
       [tags/tags-view (-> item :tags keys)]]]]))

(defn component [id user-data all-items-path filter-keys kind on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                                             hide-access-filter?
                                                                                             on-filter-string-change
                                                                                             component-styles
                                                                                             page-size
                                                                                             sort-fn
                                                                                             selected-item]}]
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
                                     filter-ownership
                                     selected-item)
        tags-to-display (->> filtered-items
                             (map (comp keys :tags))
                             (apply concat)
                             frequencies
                             (into {}))
        sorted-items (cond->> filtered-items
                       sort-fn (sort-by sort-fn))
        number-of-items-path [:ui :filter id [filter-keys filter-string filter-tags filter-access filter-ownership]]
        number-of-items (or @(rf/subscribe [:get number-of-items-path])
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
                                    (when (= kind :charge)
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
       :selected filter-tags]]

     [:div.filter-component-results
      (if (empty? display-items)
        [:div [tr :string.miscellaneous/none]]
        [:ul.filter-results
         (doall
          (for [item display-items]
            ^{:key (:id item)}
            [result-card all-items-path (:id item) kind on-select selected-item-path]))])]

     (when-not (= (count filtered-items)
                  (count display-items))
       [:div.filter-component-show-more
        [:button.button {:on-click #(rf/dispatch [::show-more
                                                  number-of-items-path
                                                  page-size])}
         [tr :string.miscellaneous/show-more]]])]))

(defn legacy-component [id user-data all-items filter-keys display-fn refresh-fn & {:keys [hide-ownership-filter?
                                                                                           hide-access-filter?
                                                                                           on-filter-string-change
                                                                                           component-styles
                                                                                           page-size
                                                                                           sort-fn
                                                                                           selected-item]}]
  (let [filter-path [:ui :filter id]
        filter-string-path (conj filter-path :filter-string)
        filter-tags-path (conj filter-path :filter-tags)
        filter-access-path (conj filter-path :filter-access)
        filter-ownership-path (conj filter-path :filter-ownership)
        filter-string @(rf/subscribe [:get filter-string-path])
        filter-tags @(rf/subscribe [:get filter-tags-path])
        filter-access @(rf/subscribe [:get filter-access-path])
        filter-ownership @(rf/subscribe [:get filter-ownership-path])
        filtered-items (filter-items user-data
                                     all-items
                                     filter-keys
                                     filter-string
                                     filter-tags
                                     filter-access
                                     filter-ownership
                                     selected-item)
        tags-to-display (->> filtered-items
                             (map (comp keys :tags))
                             (apply concat)
                             frequencies
                             (into {}))
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
