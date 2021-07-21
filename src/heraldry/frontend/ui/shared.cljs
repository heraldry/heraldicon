(ns heraldry.frontend.ui.shared
  (:require [heraldry.frontend.context :as context]))

(def coa-select-option-context
  (-> context/default
      (assoc :data {:render-options {:theme :wappenwiki
                                     :outline? true
                                     :escutcheon-shadow? true}})
      (assoc :render-options [:context :data :render-options])))
