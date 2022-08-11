(ns heraldicon.heraldry.shared.saltire
  (:require
   [heraldicon.math.vector :as v]))

(defn arm-diagonals [anchor-point orientation-point]
  (let [direction (-> (v/sub orientation-point anchor-point)
                      v/normal
                      (v/mul 30))
        direction (v/Vector. (-> direction :x Math/abs -)
                             (-> direction :y Math/abs -))]

    [(v/dot direction (v/Vector. 1 1))
     (v/dot direction (v/Vector. -1 1))
     (v/dot direction (v/Vector. 1 -1))
     (v/dot direction (v/Vector. -1 -1))]))
