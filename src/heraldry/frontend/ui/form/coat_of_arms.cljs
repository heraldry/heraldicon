(ns heraldry.frontend.ui.form.coat-of-arms
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]))

(defn form [context]
  (ui-interface/form-elements
   context
   [:escutcheon
    :manual-blazon]))

(defmethod ui-interface/component-node-data :heraldry.component/coat-of-arms [context]
  {:title :string.render-options.scope-choice/coat-of-arms
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/coat-of-arms [_context]
  {:form form})
