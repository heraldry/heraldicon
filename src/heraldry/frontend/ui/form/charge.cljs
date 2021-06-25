(ns heraldry.frontend.ui.form.charge
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path {:keys [options]}]
  [:<>
   ;; TODO: tincture modifiers need to be added, but they depend on the charge data
   (for [option [:escutcheon
                 :origin
                 :anchor
                 :geometry
                 :fimbriation]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/charge [path component-data]
  ;; TODO: if the charge has a fixed tincture, then this should prevent field config,
  ;; depends on charge data
  {:title (charge/title component-data)
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.type/charge [component-data]
  {:form form
   :form-args {:options (charge-options/options component-data)}})
