(ns heraldry.coat-of-arms.shared.chevron
  (:require
   [heraldry.math.vector :as v]))

(defn arm-diagonals [chevron-angle origin-point anchor-point]
  (let [direction (-> (v/sub anchor-point origin-point)
                      v/normal
                      (v/mul 200)
                      (v/rotate (- chevron-angle)))
        direction (if (-> direction :y neg?)
                    (v/dot direction (v/v 1 -1))
                    direction)
        direction (if (-> direction :y Math/abs (< 5))
                    (v/add direction (v/v 0 5))
                    direction)
        left (v/rotate direction chevron-angle)
        right (v/rotate (v/dot direction (v/v 1 -1)) chevron-angle)]
    [left right]))

(defn mirror-point [chevron-angle center point]
  (-> point
      (v/sub center)
      (v/rotate (- chevron-angle))
      (v/dot (v/v 1 -1))
      (v/rotate chevron-angle)
      (v/add center)))
