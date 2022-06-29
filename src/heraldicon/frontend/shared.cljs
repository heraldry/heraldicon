(ns heraldicon.frontend.shared
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.context :as context]
   [heraldicon.heraldry.default :as default]))

(def coa-select-option-context
  (-> context/default
      (c/<< :render-options (assoc default/render-options
                                   :outline? true
                                   :escutcheon-shadow? true))
      (c/<< :render-options-path [:context :render-options])))
