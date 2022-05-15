(ns heraldicon.frontend.ui.form.arms-general
  (:require
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]))

(defn form [context]
  [:<>
   (ui.interface/form-elements
    context
    [:name
     :attribution
     :is-public
     :metadata
     :tags])])

(defmethod ui.interface/component-node-data :heraldicon/arms [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod ui.interface/component-form-data :heraldicon/arms [_context]
  {:form form})
