(ns heraldry.coat-of-arms.charge.type.annulet
  (:require [heraldry.coat-of-arms.charge.shared :as charge-shared]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Annulet"
   :value         :heraldry.charge.type/annulet}
  [charge parent environment context]
  (charge-shared/make-charge
   charge parent environment context
   :width
   (fn [width]
     (let [radius      (/ width 2)
           hole-radius (* radius 0.6)]
       {:shape         ["m" (v/v radius 0)
                        ["a" radius radius
                         0 0 0 (v/v (- width) 0)]
                        ["a" radius radius
                         0 0 0 width 0]
                        "z"]
        :mask          ["m" (v/v hole-radius 0)
                        ["a" hole-radius hole-radius
                         0 0 0 (v/v (* hole-radius -2) 0)]
                        ["a" hole-radius hole-radius
                         0 0 0 (* hole-radius 2) 0]
                        "z"]
        :charge-width  width
        :charge-height width}))))
