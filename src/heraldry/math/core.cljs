(ns heraldry.math.core)

(defn close-to-zero? [value]
  (-> value
      Math/abs
      (< 0.000000000001)))

(defn to-rad [angle]
  (-> angle
      (* Math/PI)
      (/ 180)))

(defn to-deg [angle]
  (-> angle
      (/ Math/PI)
      (* 180)))

(defn normalize-angle [angle]
  (loop [angle angle]
    (cond
      (neg? angle) (recur (+ angle 360))
      (>= angle 360) (recur (- angle 360))
      :else angle)))
