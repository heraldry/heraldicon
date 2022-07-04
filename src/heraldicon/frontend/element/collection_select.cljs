(ns heraldicon.frontend.element.collection-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn component [collections-subscription on-select refresh-fn & {:keys [display-selected-item?]
                                                                  :as options}]
  [filter/component
   :collection-list
   @(rf/subscribe [::session/data])
   collections-subscription
   [:name :username :metadata :tags]
   :collection
   on-select
   refresh-fn
   (assoc options
          :page-size 20
          :component-styles (if display-selected-item?
                              {:height "80vh"}
                              {:height "90vh"}))])

(defn list-collections [on-select & {:as options}]
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/collection])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/collection])
   options])
