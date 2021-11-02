(ns heraldry.frontend.ui.form.ribbon
  (:require
   [heraldry.coat-of-arms.charge.core :as charge]
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [re-frame.core :as rf]))

(defn form [context]
  [:<>
   (for [option [:type
                 :escutcheon
                 :origin
                 :anchor
                 :geometry
                 :fimbriation
                 :tincture
                 :outline-mode
                 :vertical-mask
                 :manual-blazon]]
     ^{:key option} [ui-interface/form-element (c/++ context option)])])

(defmethod ui-interface/component-node-data :heraldry.component/charge [{:keys [path] :as context}]
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (str (charge/title context) " charge")
   :validation @(rf/subscribe [:validate-charge path])
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/charge [_context]
  {:form form})
