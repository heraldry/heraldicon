(ns heraldry.frontend.ui.form.coat-of-arms
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]))

(defn form [context]
  [:<>
   (for [option [:escutcheon
                 :manual-blazon]]
     ^{:key option} [ui-interface/form-element (c/++ context option)])])

(defmethod ui-interface/component-node-data :heraldry.component/coat-of-arms [context]
  {:title strings/coat-of-arms
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/coat-of-arms [_context]
  {:form form})
