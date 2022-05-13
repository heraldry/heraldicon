(ns heraldicon.math.core)

(defn close-to-zero? [^js/Number value]
  (-> value
      Math/abs
      (< 0.000000000001)))

(defn percent-of [^js/Number base-value]
  (fn [v]
    (when v
      (-> v
          (* base-value)
          (/ 100)))))
