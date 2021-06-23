(ns heraldry.frontend.ui.form.field
  (:require [heraldry.coat-of-arms.field.options :as field]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path {:keys [options]}]
  [:<>
   (for [option [:inherit-environment?
                 :counterchanged?
                 :type
                 :tincture
                 :line
                 :opposite-line
                 :extra-line
                 :variant
                 :thickness]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/field [_path _component-data]
  {:title "field"})

(defmethod interface/component-form-data :heraldry.type/field [component-data]
  {:form form
   :form-args {:options (field/options component-data)}})
