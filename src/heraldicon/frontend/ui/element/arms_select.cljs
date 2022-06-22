(ns heraldicon.frontend.ui.element.arms-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]))

(defn component [arms-subscription on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                  selected-arms
                                                                  display-selected-item?
                                                                  predicate-fn]}]
  [filter/component
   :arms-list
   (user/data)
   arms-subscription
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
   :display-selected-item? display-selected-item?
   :predicate-fn predicate-fn])

(defn list-arms [on-select & {:as options}]
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/arms])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/arms])
   options])
