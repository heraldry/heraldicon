(ns heraldry.frontend.ui.form.charge-general
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.element.checkbox :as checkbox]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.frontend.validation :as validation]
   [heraldry.gettext :refer [string]]))

(defn form [context]
  [:<>
   (ui-interface/form-elements
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

(defmethod ui-interface/component-node-data :heraldry.component/charge-general [context]
  {:title (string "General")
   :validation (validation/validate-charge-general context)})

(defmethod ui-interface/component-form-data :heraldry.component/charge-general [_context]
  {:form form})
