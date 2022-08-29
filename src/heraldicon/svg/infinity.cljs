(ns heraldicon.svg.infinity
  (:require
   [heraldicon.math.vector :as v]))

(def ^:private center
  (v/Vector. 50 60))

(def ^:private radius
  600)

(defn clockwise [from to & {:keys [shortest?]}]
  (let [from-ray (v/sub from center)
        to-ray (v/sub to center)
        projected-from (-> from-ray
                           (v/mul (/ radius (v/abs from-ray)))
                           (v/add center))
        projected-to (-> to-ray
                         (v/mul (/ radius (v/abs to-ray)))
                         (v/add center))
        large-arc? (if (or shortest?
                           (< (v/arc-angle-between-vectors from-ray to-ray) 180))
                     0
                     1)
        clockwise? 1]
    ["L" projected-from
     "A" radius radius 0 large-arc? clockwise? projected-to
     "L" to]))

(defn counter-clockwise [from to & {:keys [shortest?]}]
  (let [from-ray (v/sub from center)
        to-ray (v/sub to center)
        projected-from (-> from-ray
                           (v/mul (/ radius (v/abs from-ray)))
                           (v/add center))
        projected-to (-> to-ray
                         (v/mul (/ radius (v/abs to-ray)))
                         (v/add center))
        large-arc? (if (or shortest?
                           (> (v/arc-angle-between-vectors from-ray to-ray) 180))
                     0
                     1)
        clockwise? 0]
    ["L" projected-from
     "A" radius radius 0 large-arc? clockwise? projected-to
     "L" to]))
