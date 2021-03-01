(ns heraldry.frontend.form.shared
  (:require [heraldry.frontend.context :as context]))

(def coa-select-option-context
  (-> context/default
      (dissoc :fn-component-selected?)
      (dissoc :fn-select-component)))

(def ui-render-options-theme-path
  [:ui :render-options :theme])
