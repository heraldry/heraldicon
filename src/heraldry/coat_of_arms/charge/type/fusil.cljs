(ns heraldry.coat-of-arms.charge.type.fusil
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.charge.shared :as charge-shared]
   [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/fusil)

(defmethod charge-interface/display-name charge-type [_] {:en "Fusil"
                                                          :de "Wecke"})

(defmethod charge-interface/render-charge charge-type
  [context]
  (charge-shared/make-charge
   context
   :height
   (fn [height]
     (let [width (/ height 2)
           width-half (/ width 2)
           height-half (/ height 2)]
       {:shape ["m" (v/v 0 (- height-half))
                "l" (v/v width-half height-half)
                "l " (v/v (- width-half) height-half)
                "l" (v/v (- width-half) (- height-half))
                "z"]
        :charge-width width
        :charge-height height}))))
