(ns heraldicon.frontend.component.entity.charge.data
  (:require
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.validation :as validation]))

(defn- form [context]
  (element/elements
   context
   [:charge-type
    :landscape?
    :attitude
    :facing
    :colours
    :fixed-tincture
    :attributes]))

(defmethod component/node-data :heraldicon.entity.charge/data [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod component/form :heraldicon.entity.charge/data [_context]
  form)
