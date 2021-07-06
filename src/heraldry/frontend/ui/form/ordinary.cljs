(ns heraldry.frontend.ui.form.ordinary
  (:require [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path _]
  [:<>
   (for [option [:type
                 :variant
                 :line
                 :opposite-line
                 :escutcheon
                 :num-points
                 :angle
                 :origin
                 :direction-anchor
                 :anchor
                 :geometry
                 :fimbriation
                 :cottising
                 :outline?]]
     ^{:key option} [interface/form-element (conj path option)])])

(defmethod interface/component-node-data :heraldry.component/ordinary [path component-data]
  {:title (ordinary/title component-data)
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.component/ordinary [_component-data]
  {:form form})
