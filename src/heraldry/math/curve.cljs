(ns heraldry.math.curve
  (:require [heraldry.math.bezier :as bezier]))

(defn length [curve]
  (->> curve
       (map bezier/length)
       (apply +)))

(defn split [curve t]
  (let [total-length (length curve)
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
