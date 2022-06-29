(ns heraldicon.frontend.component.coat-of-arms
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.element.core :as element]))

(defn- form [context]
  (element/elements
   context
   [:escutcheon
    :manual-blazon]))

(defmethod component/node-data :heraldry/coat-of-arms [context]
  {:title :string.render-options.scope-choice/coat-of-arms
   :nodes [{:context (c/++ context :field)}]})

(defmethod component/form-data :heraldry/coat-of-arms [_context]
  form)
