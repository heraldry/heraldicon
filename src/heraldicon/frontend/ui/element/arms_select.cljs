(ns heraldicon.frontend.ui.element.arms-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.loading :as loading]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]))

(defn component [arms-list-path on-select refresh-fn & {:keys [hide-ownership-filter?
                                                               selected-arms
                                                               display-selected-item?]}]
  [filter/component
   :arms-list
   (user/data)
   arms-list-path
   [:name :username :metadata :tags]
   :arms
   on-select
   refresh-fn
   :sort-fn (juxt (comp filter/normalize-string-for-sort :name)
                  :type
                  :id
                  :version)
   :page-size 20
   :hide-ownership-filter? hide-ownership-filter?
   :component-styles (if display-selected-item?
                       {:height "80vh"}
                       {:height "90vh"})
   :selected-item selected-arms
   :display-selected-item? display-selected-item?])

(defn list-arms [on-select & {:keys [selected-arms
                                     display-selected-item?]}]
  (let [{:keys [status path]} @(rf/subscribe [::entity-list/data :heraldicon.entity.type/arms])]
    (if (= status :done)
      [component
       path
       on-select
       #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/arms])
       :selected-arms selected-arms
       :display-selected-item? display-selected-item?]
      [loading/loading])))
