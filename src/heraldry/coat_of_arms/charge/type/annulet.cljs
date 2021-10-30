(ns heraldry.coat-of-arms.charge.type.annulet
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.charge.shared :as charge-shared]
   [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/annulet)

(defmethod charge-interface/display-name charge-type [_] {:en "Annulet"
                                                          :de "Ring"})

(defmethod charge-interface/render-charge charge-type
  [path environment context]
  (charge-shared/make-charge
   path environment context
   :width
   (fn [width]
     (let [radius (/ width 2)
           hole-radius (* radius 0.6)]
       {:shape ["m" (v/v radius 0)
                ["a" radius radius
                 0 0 0 (v/v (- width) 0)]
                ["a" radius radius
                 0 0 0 width 0]
                "z"]
        :mask ["m" (v/v hole-radius 0)
               ["a" hole-radius hole-radius
                0 0 0 (v/v (* hole-radius -2) 0)]
               ["a" hole-radius hole-radius
                0 0 0 (* hole-radius 2) 0]
               "z"]
        :charge-width width
        :charge-height width}))))
