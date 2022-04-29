(ns heraldicon.frontend.ui.shared
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.context :as context]))

(def coa-select-option-context
  (-> context/default
      (c/<< :render-options {:theme :wappenwiki
                             :outline? true
                             :escutcheon-shadow? true})
      (c/<< :render-options-path [:context :render-options])))
