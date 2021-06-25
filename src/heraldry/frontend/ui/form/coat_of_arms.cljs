(ns heraldry.frontend.ui.form.coat-of-arms
  (:require [heraldry.coat-of-arms.core :as coat-of-arms]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path {:keys [options]}]
  [:<>
   (for [option [:escutcheon]]
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/coat-of-arms [path _component-data]
  {:title "Coat of Arms"
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.type/coat-of-arms [component-data]
  {:form form
   :form-args {:options (coat-of-arms/options component-data)}})
