(ns heraldry.frontend.ui.form.charge
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:type
                 :escutcheon
                 :origin
                 :anchor
                 :geometry
                 :fimbriation
                 :tincture
                 :outline-mode]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/charge [path]
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (str (charge/title path) " charge")
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.component/charge [_path _component-data _component-options]
  {:form form})
