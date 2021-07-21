(ns heraldry.coat-of-arms.charge.type.billet
  (:require [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.vector :as v]))

(def charge-type :heraldry.charge.type/billet)

(defmethod charge-interface/display-name charge-type [_] "Billet")

(defmethod charge-interface/render-charge charge-type
  [path parent-path environment context]
  (charge-shared/make-charge
   path parent-path environment context
   :height
   (fn [height]
     (let [width (/ height 2)
           width-half (/ width 2)
           height-half (/ height 2)]
       {:shape ["m" (v/v (- width-half) (- height-half))
                "h" width
                "v" height
                "h" (- width)
                "z"]
        :charge-width width
        :charge-height height}))))
