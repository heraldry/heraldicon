(ns heraldry.frontend.ui.form.semy
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:origin
                 :layout]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/semy [path component-data]
  {:title (str "Semy of " (charge/title (:charge component-data)))
   :nodes [{:path (conj path :charge)}]})

(defmethod interface/component-form-data :heraldry.component/semy [_component-data]
  {:form form})
