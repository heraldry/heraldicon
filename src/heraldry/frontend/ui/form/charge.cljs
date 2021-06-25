(ns heraldry.frontend.ui.form.charge
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path {:keys [options]}]
  [:<>
   (for [option [:escutcheon
                 :geometry
                 :fimbriation]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/charge [path component-data]
  {:title (charge/title component-data)
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.type/charge [component-data]
  {:form form
   :form-args {:options (charge-options/options component-data)}})
