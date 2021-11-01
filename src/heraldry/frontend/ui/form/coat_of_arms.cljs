(ns heraldry.frontend.ui.form.coat-of-arms
  (:require
   [heraldry.frontend.ui.interface :as interface]
   [heraldry.strings :as strings]))

(defn form [path]
  [:<>
   (for [option [:escutcheon
                 :manual-blazon]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/coat-of-arms [context]
  {:title strings/coat-of-arms
   :nodes [{:context (update context :path conj :field)}]})

(defmethod interface/component-form-data :heraldry.component/coat-of-arms [_context]
  {:form form})
