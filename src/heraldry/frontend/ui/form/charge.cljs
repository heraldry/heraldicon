(ns heraldry.frontend.ui.form.charge
  (:require
   [heraldry.coat-of-arms.charge.options :as charge-options]
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [re-frame.core :as rf]))

(defn form [context]
  (ui-interface/form-elements
   context
   [:type
    :escutcheon
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
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (charge-options/title context)
   :validation @(rf/subscribe [:validate-charge context])
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/charge [_context]
  {:form form})
