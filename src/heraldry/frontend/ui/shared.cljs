(ns heraldry.frontend.ui.shared
  (:require [heraldry.frontend.context :as context]
            [heraldry.frontend.state :as state]))

(def coa-select-option-context
  (-> context/default
      (assoc :data {:render-options {:theme :wappenwiki
                                     :outline? true
                                     :escutcheon-shadow? true}})
      (assoc :render-options [:render-options])
      (assoc :access state/access-by-context)))
