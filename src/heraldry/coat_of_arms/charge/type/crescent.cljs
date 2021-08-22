(ns heraldry.coat-of-arms.charge.type.crescent
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.vector.core :as v]))

(def charge-type :heraldry.charge.type/crescent)

(defmethod charge-interface/display-name charge-type [_] "Crescent")

(defmethod charge-interface/render-charge charge-type
  [path parent-path environment context]
  (charge-shared/make-charge
   path parent-path environment context
   :width
   (fn [width]
     (let [radius (/ width 2)
           inner-radius (* radius
                           0.75)
           horn-angle -45
           horn-point-x (* radius
                           (-> horn-angle
                               (* Math/PI)
                               (/ 180)
                               Math/cos))
           horn-point-y (* radius
                           (-> horn-angle
                               (* Math/PI)
                               (/ 180)
                               Math/sin))
           horn-point-1 (v/v horn-point-x horn-point-y)
           horn-point-2 (v/v (- horn-point-x) horn-point-y)]
       {:shape ["m" horn-point-1
                ["a" radius radius
                 0 1 1 (v/- horn-point-2 horn-point-1)]
                ["a" inner-radius inner-radius
                 0 1 0 (v/- horn-point-1 horn-point-2)]
                "z"]
        :charge-width width
        :charge-height width}))))
