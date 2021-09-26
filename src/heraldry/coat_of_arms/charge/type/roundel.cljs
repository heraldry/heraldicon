(ns heraldry.coat-of-arms.charge.type.roundel
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/roundel)

(defmethod charge-interface/display-name charge-type [_] {:en "Roundel"
                                                          :de "Kugel"})

(defmethod charge-interface/render-charge charge-type
  [path parent-path environment context]
  (charge-shared/make-charge
   path parent-path environment context
   :width
   (fn [width]
     (let [radius (/ width 2)]
       {:shape ["m" (v/v radius 0)
                ["a" radius radius
                 0 0 0 (v/v (- width) 0)]
                ["a" radius radius
                 0 0 0 width 0]
                "z"]
        :charge-width width
        :charge-height width}))))
