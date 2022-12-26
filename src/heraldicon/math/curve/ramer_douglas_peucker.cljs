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

(comment
  (println :solution (ramer-douglas-peucker
                      (mapv (fn [[x y]]
                              (v/Vector. x y)) [[1 2] [2 3] [3 4] [4 6] [4 2]])
                      0.5))

  ;;
  )
