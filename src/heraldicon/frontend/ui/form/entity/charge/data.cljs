(ns heraldicon.frontend.ui.form.entity.charge.data
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.ui.element.checkbox :as checkbox]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]))

(defn form [context]
  [:<>
   (ui.interface/form-elements
    context
    [:type
     :landscape?
     :attitude
     :facing
     :colours
     :fixed-tincture
     :attributes])

   ;; TODO: not ideal, probably should move this at some point
   [checkbox/checkbox (c/<< context :path [:example-coa :render-options :preview-original?])]])

(defmethod ui.interface/component-node-data :heraldicon.charge/data [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod ui.interface/component-form-data :heraldicon.charge/data [_context]
  {:form form})
