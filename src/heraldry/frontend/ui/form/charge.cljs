(ns heraldry.frontend.ui.form.charge
  (:require
   [heraldry.coat-of-arms.charge.options :as charge-options]
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.static :as static]
   [re-frame.core :as rf]))

(defn form [context]
  (ui-interface/form-elements
   context
   [:type
    :escutcheon
    :flag-aspect-ratio-preset
    :flag-height
    :flag-width
    :origin
    :anchor
    :geometry
    :fimbriation
    :tincture
    :outline-mode
    :vertical-mask
    :manual-blazon
    :ignore-layer-separator?]))

(defmethod ui-interface/component-node-data :heraldry.component/charge [context]
  (let [charge-type (interface/get-raw-data (c/++ context :type))]
    ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
    ;; depends on charge data
    {:title (charge-options/title context)
     :icon (if (-> charge-type charge-options/choice-map)
             {:default (static/static-url
                        (str "/svg/charge-type-" (name charge-type) "-unselected.svg"))
              :selected (static/static-url
                         (str "/svg/charge-type-" (name charge-type) "-selected.svg"))}
             {:default (static/static-url
                        (str "/svg/charge-type-roundel-unselected.svg"))
              :selected (static/static-url
                         (str "/svg/charge-type-roundel-selected.svg"))})
     :validation @(rf/subscribe [:validate-charge context])
     :nodes [{:context (c/++ context :field)}]}))

(defmethod ui-interface/component-form-data :heraldry.component/charge [_context]
  {:form form})
