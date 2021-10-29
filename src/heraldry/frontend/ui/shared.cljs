(ns heraldry.frontend.ui.shared
  (:require
   [heraldry.frontend.context :as context]))

(def coa-select-option-context
  (-> context/default
      (assoc :render-options {:theme :wappenwiki
                              :outline? true
                              :escutcheon-shadow? true})
      (assoc :render-options-path [:context :render-options])))
