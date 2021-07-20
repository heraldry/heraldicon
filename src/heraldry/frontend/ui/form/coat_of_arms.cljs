(ns heraldry.frontend.ui.form.coat-of-arms
  (:require [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:escutcheon]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/coat-of-arms [path]
  {:title "Coat of Arms"
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.component/coat-of-arms [_path]
  {:form form})
