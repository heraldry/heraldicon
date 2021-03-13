(ns heraldry.coat-of-arms.shared.saltire
  (:require [heraldry.coat-of-arms.vector :as v]))

(defn arm-diagonals [origin-point anchor-point]
  (let [direction (-> (v/- anchor-point origin-point)
                      v/normal
                      (v/* 200))
        direction (v/v (-> direction :x Math/abs -)
                       (-> direction :y Math/abs -))]

    [(v/dot direction (v/v 1 1))
     (v/dot direction (v/v -1 1))
     (v/dot direction (v/v 1 -1))
     (v/dot direction (v/v -1 -1))]))
