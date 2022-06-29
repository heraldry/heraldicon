(ns heraldicon.frontend.component.entity.charge.data
  (:require
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]))

(defn- form [context]
  (ui.interface/form-elements
   context
   [:charge-type
    :landscape?
    :attitude
    :facing
    :colours
    :fixed-tincture
    :attributes]))

(defmethod ui.interface/component-node-data :heraldicon.entity.charge/data [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod ui.interface/component-form-data :heraldicon.entity.charge/data [_context]
  {:form form})
