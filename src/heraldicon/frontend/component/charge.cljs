(ns heraldicon.frontend.component.charge
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.ui.element.charge-type-select :as charge-type-select]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.interface :as interface]))

(defn- form [context]
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

(defmethod ui.interface/component-node-data :heraldry/charge [context]
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (charge.options/title context)
   :icon (let [url (charge-type-select/choice-preview-url context)]
           {:default url
            :selected url})
   :validation (validation/validate-charge context)
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui.interface/component-form-data :heraldry/charge [_context]
  {:form form})
