(ns heraldry.coat-of-arms.charge.type.fusil
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.vector.core :as v]))

(def charge-type :heraldry.charge.type/fusil)

(defmethod charge-interface/display-name charge-type [_] "Fusil")

(defmethod charge-interface/render-charge charge-type
  [path parent-path environment context]
  (charge-shared/make-charge
   path parent-path environment context
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
