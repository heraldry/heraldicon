(ns heraldry.coat-of-arms.shared.saltire
  (:require
   [heraldry.math.vector :as v]))

(defn arm-diagonals [anchor-point orientation-point]
  (let [direction (-> (v/sub orientation-point anchor-point)
                      v/normal
                      (v/mul 200))
        direction (v/v (-> direction :x Math/abs -)
                       (-> direction :y Math/abs -))]

    [(v/dot direction (v/v 1 1))
     (v/dot direction (v/v -1 1))
     (v/dot direction (v/v 1 -1))
     (v/dot direction (v/v -1 -1))]))
