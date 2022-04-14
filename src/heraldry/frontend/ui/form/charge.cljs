(ns heraldry.frontend.ui.form.charge
  (:require
   [heraldry.coat-of-arms.charge.options :as charge.options]
   [heraldry.context :as c]
   [heraldry.frontend.ui.element.charge-type-select :as charge-type-select]
   [heraldry.frontend.ui.interface :as ui.interface]
   [heraldry.frontend.validation :as validation]
   [heraldry.interface :as interface]))

(defn form [context]
  [:<>
   (when-not (interface/get-raw-data (c/++ context :preview?))
     [ui.interface/form-element (c/++ context :type)])

   (ui.interface/form-elements
    context
    [:escutcheon
     :flag-aspect-ratio-preset
     :flag-height
     :flag-width
     :flag-swallow-tail
     :flag-tail-point-height
     :flag-tail-tongue
     :anchor
     :orientation
     :geometry
     :num-points
     :eccentricity
     :wavy-rays?
     :fimbriation
     :tincture
     :outline-mode
     :vertical-mask
     :manual-blazon
     :ignore-layer-separator?])])

(defmethod ui.interface/component-node-data :heraldry.component/charge [context]
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (charge.options/title context)
   :icon (let [url (charge-type-select/choice-preview-url context)]
           {:default url
            :selected url})
   :validation (validation/validate-charge context)
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui.interface/component-form-data :heraldry.component/charge [_context]
  {:form form})
