(ns heraldicon.frontend.element.arms-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn component [arms-subscription on-select refresh-fn & {:keys [display-selected-item?
                                                                  list-id]
                                                           :as options}]
  [filter/component
   (or list-id :arms-list)
   @(rf/subscribe [::session/data])
   arms-subscription
   [[:name]
    [:username]
    [:metadata]
    [:tags]]
   :arms
   on-select
   refresh-fn
   (assoc options
          :page-size 20
          :component-styles (if display-selected-item?
                              {:height "80vh"}
                              {:height "90vh"}))])

(defn list-arms [on-select & {:as options}]
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/arms])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/arms])
   options])
