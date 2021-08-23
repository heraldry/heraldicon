(ns heraldry.coat-of-arms.charge.type.mascle
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/mascle)

(defmethod charge-interface/display-name charge-type [_] "Mascle")

(defmethod charge-interface/render-charge charge-type
  [path parent-path environment context]
  (charge-shared/make-charge
   path parent-path environment context
   :height
   (fn [height]
     (let [width (/ height 1.3)
           width-half (/ width 2)
           height-half (/ height 2)
           hole-width (* width 0.55)
           hole-height (* height 0.55)
           hole-width-half (/ hole-width 2)
           hole-height-half (/ hole-height 2)]
       {:shape ["m" (v/v 0 (- height-half))
                "l" (v/v width-half height-half)
                "l " (v/v (- width-half) height-half)
                "l" (v/v (- width-half) (- height-half))
                "z"]
        :mask ["m" (v/v 0 (- hole-height-half))
               "l" (v/v hole-width-half hole-height-half)
               "l " (v/v (- hole-width-half) hole-height-half)
               "l" (v/v (- hole-width-half) (- hole-height-half))
               "z"]
        :charge-width width
        :charge-height height}))))
