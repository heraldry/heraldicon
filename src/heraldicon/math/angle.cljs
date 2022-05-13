(ns heraldicon.math.angle)

(defn to-rad ^js/Number [^js/Number angle]
  (-> angle
      (* Math/PI)
      (/ 180)))

(defn to-deg ^js/Number [^js/Number angle]
  (-> angle
      (/ Math/PI)
      (* 180)))

(defn normalize ^js/Number [^js/Number angle]
  (loop [angle angle]
    (cond
      (neg? angle) (recur (+ angle 360))
      (>= angle 360) (recur (- angle 360))
      :else angle)))
