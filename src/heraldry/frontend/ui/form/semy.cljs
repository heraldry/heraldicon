(ns heraldry.frontend.ui.form.semy
  (:require [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:origin
                 :layout
                 :rectangular?
                 :manual-blazon]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/semy [path]
  {:title (str "Semy of " (charge-options/title (conj path :charge) {}))
   :nodes [{:path (conj path :charge)}]})

(defmethod interface/component-form-data :heraldry.component/semy [_path]
  {:form form})
