(ns heraldicon.math.core)

(defn close-to-zero? [^js/Number value]
  (< (Math/abs value) 0.000000000001))

(defn percent-of [^js/Number base-value v]
  (when v
    (/ (* v base-value) 100)))
