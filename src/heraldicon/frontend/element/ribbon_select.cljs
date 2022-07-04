(ns heraldicon.frontend.element.ribbon-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn- component [ribbons-subscription on-select refresh-fn & {:keys [display-selected-item?]
                                                               :as options}]
  [filter/component
   :ribbon-list
   @(rf/subscribe [::session/data])
   ribbons-subscription
   [:name :username :metadata :tags]
   :ribbon
   on-select
   refresh-fn
   (assoc options
          :page-size 20
          :component-styles (if display-selected-item?
                              {:height "80vh"}
                              {:height "90vh"}))])

(defn list-ribbons [on-select & {:as options}]
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/ribbon])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/ribbon])
   options])
