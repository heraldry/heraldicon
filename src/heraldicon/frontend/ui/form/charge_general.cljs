(ns heraldicon.frontend.ui.form.charge-general
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.ui.element.checkbox :as checkbox]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]))

(defn form [context]
  [:<>
   (ui.interface/form-elements
    context
    [:name
     :attribution
     :is-public
     :type
     :landscape?
     :attitude
     :facing
     :colours
     :fixed-tincture
     :attributes
     :metadata
     :tags])

   ;; TODO: not ideal, probably should move this at some point
   [checkbox/checkbox (c/<< context :path [:example-coa :render-options :preview-original?])]])

(defmethod ui.interface/component-node-data :heraldry/charge-general [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-charge-general context)})

(defmethod ui.interface/component-form-data :heraldry/charge-general [_context]
  {:form form})
