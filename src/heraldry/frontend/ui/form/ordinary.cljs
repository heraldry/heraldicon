(ns heraldry.frontend.ui.form.ordinary
  (:require [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.frontend.ui.interface :as interface]))

(defn form [path {:keys [options]}]
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
     ^{:key option} [interface/form-element (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/ordinary [path component-data]
  {:title (ordinary/title component-data)
   :nodes [{:path (conj path :field)}]})

(defmethod interface/component-form-data :heraldry.type/ordinary [component-data]
  {:form form
   :form-args {:options (ordinary-options/options component-data)}})
