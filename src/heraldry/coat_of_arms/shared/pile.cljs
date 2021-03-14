(ns heraldry.coat-of-arms.shared.pile
  (:require [heraldry.coat-of-arms.vector :as v]))

(defn diagonals [origin-point anchor-point width stretch anchor environment]
  (let [type                 (or (:type anchor)
                                 :edge)
        target-point         (case type
                               :edge (v/find-intersection
                                      origin-point anchor-point
                                      environment)
                               anchor-point)
        direction-vector     (v/- target-point origin-point)
        direction-length     (v/abs direction-vector)
        direction            (v// direction-vector direction-length)
        length               (or stretch 1)
        point                (-> direction
                                 (v/* (* direction-length length))
                                 (v/+ origin-point))
        direction-orthogonal (v/orthogonal direction)
        left-point           (v/+ origin-point
                                  (v/* direction-orthogonal (/ width 2)))
        right-point          (v/- origin-point
                                  (v/* direction-orthogonal (/ width 2)))]
    {:left  (-> left-point
                (v/- point)
                v/normal
                (v/* 500)
                (v/+ point))
     :right (-> right-point
                (v/- point)
                v/normal
                (v/* 500)
                (v/+ point))
     :point point}))
