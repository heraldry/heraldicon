(ns heraldry.math.catmullrom
  (:require [clojure.string :as string]
            [heraldry.math.bezier :as bezier]
            [heraldry.math.svg.path :as path]
            [heraldry.math.vector :as v]))

(defn smooth-point [main-fn p0 p1 p2 tension]
  (main-fn p1
           (v/mul (v/sub p2 p0)
                  (/ tension 6))))

(defn calculate-cubic-bezier-curve
  [tension [p0 p1 p2 p3]]
  (bezier/bezier p1
                 (smooth-point v/add p0 p1 p2 tension)
                 (smooth-point v/sub p1 p2 p3 tension)
                 p2))

(defn catmullrom
  [points & {:keys [tension] :or {tension 1}}]
  (->> (concat [(first points)] points [(last points)])
       (partition 4 1)
       (map (partial calculate-cubic-bezier-curve tension))))

;; svg

(defn curve->svg-path-relative [curve]
  (let [start (first (first curve))]
    (string/join "" (concat [(path/move-to start)]
                            (map path/bezier-to-relative curve)))))

(defn curve->length [path]
  (->> path
       (map bezier/length)
       (apply +)))

(defn split-curve-at [curve t]
  (let [total-length (curve->length curve)
        num-curves (count curve)
        absolute-t (* t total-length)
        [split-index
         rest-t
         _] (->> curve
                 (map bezier/length)
                 (map-indexed vector)
                 (reduce
                  (fn [[_ _ traversed] [index leg-length]]
                    (let [next-traversed (+ traversed leg-length)]
                      (if (> absolute-t next-traversed)
                        [index 1 next-traversed]
                        (reduced [index
                                  (/ (- absolute-t traversed) leg-length)
                                  absolute-t]))))
                  [0 0 0]))
        rest-split (bezier/split (get curve split-index) rest-t)]
    {:curve1 (vec (concat (if (pos? split-index)
                            (subvec curve 0 split-index)
                            [])
                          [(:bezier1 rest-split)]))
     :curve2 (vec (concat [(:bezier2 rest-split)]
                          (if (< split-index (dec num-curves))
                            (subvec curve (inc split-index))
                            [])))}))
