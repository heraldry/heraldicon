(ns heraldicon.frontend.ui.element.collection-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn component [collections-subscription on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                         selected-collection
                                                                         display-selected-item?
                                                                         predicate-fn]}]
  [filter/component
   :collection-list
   @(rf/subscribe [::session/data])
   collections-subscription
   [:name :username :metadata :tags]
   :collection
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
   :selected-item selected-collection
   :display-selected-item? display-selected-item?
   :predicate-fn predicate-fn])

(defn list-collections [on-select & {:as options}]
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/collection])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/collection])
   options])
