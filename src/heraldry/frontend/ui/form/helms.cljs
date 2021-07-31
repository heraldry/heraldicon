(ns heraldry.frontend.ui.form.helms
  (:require [heraldry.frontend.ui.interface :as ui-interface]))

(defn form [path _]
  [:<>
   (for [option []]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defmethod ui-interface/component-node-data :heraldry.component/helms [path]
  {:title "Helms and crests"
   :buttons []
   :nodes []})

(defmethod ui-interface/component-form-data :heraldry.component/helms [_path]
  {:form form})
