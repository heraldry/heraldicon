(ns heraldry.coat-of-arms.charge.type.lozenge
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.charge.shared :as charge-shared]
   [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/lozenge)

(defmethod charge-interface/display-name charge-type [_] {:en "Lozenge"
                                                          :de "Raute"})

(defmethod charge-interface/render-charge charge-type
  [path environment context]
  (charge-shared/make-charge
   path environment context
   :height
   (fn [height]
     (let [width (/ height 1.3)
           width-half (/ width 2)
           height-half (/ height 2)]
       {:shape ["m" (v/v 0 (- height-half))
                "l" (v/v width-half height-half)
                "l " (v/v (- width-half) height-half)
                "l" (v/v (- width-half) (- height-half))
                "z"]
        :charge-width width
        :charge-height height}))))
