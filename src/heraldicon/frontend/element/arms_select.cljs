(ns heraldicon.frontend.element.arms-select
  (:require
   [heraldicon.frontend.search-filter :as search-filter]))

(defn component [on-select & {:keys [display-selected-item?
                                     list-id]
                              :as options}]
  [search-filter/component
   (or list-id :arms-list)
   :arms
   on-select
   (assoc options
          :page-size 20
          :component-styles (if display-selected-item?
                              {:height "80vh"}
                              {:height "90vh"}))])

(defn list-arms [on-select & {:as options}]
  [component on-select options])
