(ns heraldry.coat-of-arms.charge.type.lozenge
  (:require [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Lozenge"
   :value :lozenge}
  [charge parent environment context]
  (charge-shared/make-charge
   charge parent environment context
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
