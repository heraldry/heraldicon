(ns heraldry.frontend.ui.shared
  (:require [heraldry.frontend.context :as context]))

(def coa-select-option-context
  (-> context/default
      (dissoc :fn-component-selected?)
      (dissoc :fn-select-component)
      (assoc :render-options {:theme :wappenwiki
                              :outline? true
                              :escutcheon-shadow? true})))
