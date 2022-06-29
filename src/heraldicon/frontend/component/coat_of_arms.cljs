(ns heraldicon.frontend.component.coat-of-arms
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.interface :as ui.interface]))

(defn- form [context]
  (ui.interface/form-elements
   context
   [:escutcheon
    :manual-blazon]))

(defmethod ui.interface/component-node-data :heraldry/coat-of-arms [context]
  {:title :string.render-options.scope-choice/coat-of-arms
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui.interface/component-form-data :heraldry/coat-of-arms [_context]
  {:form form})
