(ns heraldry.frontend.ui.shared
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.context :as context]))

(def coa-select-option-context
  (-> context/default
      (c/<< :render-options {:theme :wappenwiki
                             :outline? true
                             :escutcheon-shadow? true})
      (c/<< :render-options-path [:context :render-options])))
