(ns heraldicon.frontend.element.charge-select
  (:require
   [heraldicon.frontend.search-filter :as search-filter]))

(defn component [on-select & {:keys [display-selected-item?
                                     list-id]
                              :as options}]
  [search-filter/component
   (or list-id :charge-list)
   :charge
   on-select
   (assoc options
          :page-size 20
          :component-styles (if display-selected-item?
                              {:height "75vh"}
                              {:height "90vh"}))])

(defn list-charges [on-select & {:as options}]
  [component on-select options])
