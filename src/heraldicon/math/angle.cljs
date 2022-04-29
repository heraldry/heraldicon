(ns heraldicon.math.angle)

(defn to-rad [angle]
  (-> angle
      (* Math/PI)
      (/ 180)))

(defn to-deg [angle]
  (-> angle
      (/ Math/PI)
      (* 180)))

(defn normalize [angle]
  (loop [angle angle]
    (cond
      (neg? angle) (recur (+ angle 360))
      (>= angle 360) (recur (- angle 360))
      :else angle)))
