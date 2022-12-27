(ns heraldicon.math.curve.ramer-douglas-peucker
  (:require
   [heraldicon.math.vector :as v]))

(defn ramer-douglas-peucker [points epsilon]
  (if (< (count points) 3)
    points
    (let [first-point (first points)
          last-point (last points)
          point-distances (map-indexed (fn [index point]
                                         [index (v/distance-point-to-line point first-point last-point)]) points)
          [max-index max-distance] (apply max-key second point-distances)]
      (if (< max-distance epsilon)
        [first-point last-point]
        (into []
              (concat (drop-last (ramer-douglas-peucker (take (inc max-index) points) epsilon))
                      (ramer-douglas-peucker (drop max-index points) epsilon)))))))

(defn- point-strictly-left-of-line? [point first-point last-point]
  (let [value (v/cross (v/sub first-point point)
                       (v/sub last-point point))]
    (neg? value)))

(defn ramer-douglas-peucker-convex
  "Like ramer-douglas-peucker, but area 'right' of the path is never cropped.

  This is achieved by always prioritizing 'left' distances and only cutting points
  if none of them are 'left'."
  [points epsilon]
  (if (< (count points) 3)
    points
    (let [first-point (first points)
          last-point (last points)
          point-distances (map-indexed (fn [index point]
                                         [index
                                          (v/distance-point-to-line point first-point last-point)
                                          (point-strictly-left-of-line? point first-point last-point)]) points)
          [max-index max-distance max-left?] (apply max-key
                                                    (fn [[idx dist left?]]
                                                      [(if left?
                                                         1
                                                         0)
                                                       dist
                                                       (- idx)])
                                                    point-distances)]
      (if (and (< max-distance epsilon)
               (not max-left?))
        [first-point last-point]
        (into []
              (concat (drop-last (ramer-douglas-peucker-convex (take (inc max-index) points) epsilon))
                      (ramer-douglas-peucker-convex (drop max-index points) epsilon)))))))
