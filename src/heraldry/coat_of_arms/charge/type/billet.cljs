(ns heraldry.coat-of-arms.charge.type.billet
  (:require [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Billet"
   :value        :billet}
  [charge parent environment context]
  (charge-shared/make-charge
   charge parent environment context
   :height
   (fn [height]
     (let [width       (/ height 2)
           width-half  (/ width 2)
           height-half (/ height 2)]
       {:shape         ["m" (v/v (- width-half) (- height-half))
                        "h" width
                        "v" height
                        "h" (- width)
                        "z"]
        :charge-width  width
        :charge-height height}))))
