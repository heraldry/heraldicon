(ns heraldry.frontend.ui.form.charge
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.util :as util]))

(defn form [path _]
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
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/charge [path]
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (charge-options/title path {})
   :validation @(rf/subscribe [:validate-charge path])
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.component/charge [_path]
  {:form form})
