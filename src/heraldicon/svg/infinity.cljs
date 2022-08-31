(ns heraldicon.svg.infinity
  (:require
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]))

(def ^:private radius
  600)

(defn clockwise [bounding-box from to & {:keys [shortest?]}]
  (let [center (if (instance? v/Vector bounding-box)
                 bounding-box
                 (bb/center bounding-box))
        from-ray (v/sub from center)
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

(defn counter-clockwise [bounding-box from to & {:keys [shortest?]}]
  (let [center (if (instance? v/Vector bounding-box)
                 bounding-box
                 (bb/center bounding-box))
        from-ray (v/sub from center)
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

(defn full [bounding-box]
  (let [center (if (instance? v/Vector bounding-box)
                 bounding-box
                 (bb/center bounding-box))
        up (v/add center (v/Vector. 0 radius))
        down (v/add center (v/Vector. 0 (- radius)))]
    ["M" up
     "A" radius radius 0 0 1 down
     "A" radius radius 0 0 1 up
     "z"]))
