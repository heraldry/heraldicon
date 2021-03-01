(ns heraldry.coat-of-arms.charge.type.rustre
  (:require [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Rustre"
   :value        :rustre}
  [charge parent environment context]
  (charge-shared/make-charge
   charge parent environment context
   :height
   (fn [height]
     (let [width       (/ height 1.3)
           width-half  (/ width 2)
           height-half (/ height 2)
           hole-radius (/ width 4)]
       {:shape         ["m" (v/v 0 (- height-half))
                        "l" (v/v width-half height-half)
                        "l " (v/v (- width-half) height-half)
                        "l" (v/v (- width-half) (- height-half))
                        "z"]
        :mask          ["m" (v/v hole-radius 0)
                        ["a" hole-radius hole-radius
                         0 0 0 (v/v (* hole-radius -2) 0)]
                        ["a" hole-radius hole-radius
                         0 0 0 (* hole-radius 2) 0]
                        "z"]
        :charge-width  width
        :charge-height height}))))
