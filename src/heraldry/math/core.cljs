(ns heraldry.math.core)

(defn close-to-zero? [value]
  (-> value
      Math/abs
      (< 0.000000000001)))
