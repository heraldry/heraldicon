(ns heraldicon.frontend.element.collection-select
  (:require
   [heraldicon.frontend.search-filter :as search-filter]))

(defn component [on-select & {:keys [display-selected-item?
                                     list-id]
                              :as options}]
  [search-filter/component
   (or list-id :collection-list)
   :collection
   on-select
   (assoc options
          :page-size 20
          :component-styles (if display-selected-item?
                              {:height "80vh"}
                              {:height "90vh"})
          :title-fn (fn [entity]
                      ;; TODO: include the arms count, once the list entity has that information
                      (:name entity)))])

(defn list-collections [on-select & {:as options}]
  [component on-select options])
