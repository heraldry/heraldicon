(ns heraldry.coat-of-arms.charge.type.roundel
  (:require [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Roundel"
   :value :roundel}
  [charge parent environment context]
  (charge-shared/make-charge
   charge parent environment context
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
