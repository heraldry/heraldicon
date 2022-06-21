(ns heraldicon.frontend.ui.element.charge-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.loading :as loading]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.ui.element.blazonry-editor :as blazonry-editor]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]))

(defn component [charge-list-path on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                 selected-charge
                                                                 display-selected-item?]}]
  (let [user-data (user/data)]
    [filter/component
     :charge-list
     user-data
     charge-list-path
     [:name :username :metadata :tags
      [:data :charge-type] [:data :attitude] [:data :facing] [:data :attributes] [:data :colours]]
     :charge
     on-select
     refresh-fn
     :sort-fn (juxt (comp filter/normalize-string-for-sort :name)
                    #(-> % :data :charge-type)
                    :id
                    :version)
     :page-size 20
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles (if display-selected-item?
                         {:height "75vh"}
                         {:height "90vh"})
     :selected-item selected-charge
     :display-selected-item? display-selected-item?]))

(defn list-charges [on-select & {:keys [selected-charge
                                        display-selected-item?]}]
  (let [{:keys [status path]} @(rf/subscribe [::entity-list/data :heraldicon.entity.type/charge blazonry-editor/update-parser])]
    (if (= status :done)
      [component
       path
       on-select
       #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/charge])
       :selected-charge selected-charge
       :display-selected-item? display-selected-item?]
      [loading/loading])))
