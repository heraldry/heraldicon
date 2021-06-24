(ns heraldry.frontend.ui.form.field
  (:require [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.field.options :as field-options]
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
                 :thickness
                 :layout]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/field [_path component-data]
  {:title (field/title component-data)})

(defmethod interface/component-form-data :heraldry.type/field [component-data]
  {:form form
   :form-args {:options (field-options/options component-data)}})
