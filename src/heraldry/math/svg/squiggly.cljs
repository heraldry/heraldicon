(ns heraldry.math.svg.squiggly
  (:require [clojure.walk :as walk]
            [heraldry.math.catmullrom :as catmullrom]
            [heraldry.math.svg.path :as path]
            [heraldry.math.vector :as v]
            [heraldry.random :as random]))

(defn jiggle [[previous
               {:keys [x y] :as current}
               _]]
  (let [dist (-> current
                 (v/sub previous)
                 (v/abs))
        jiggle-radius (/ dist 4)
        dx (- (* (random/float) jiggle-radius)
              jiggle-radius)
        dy (- (* (random/float) jiggle-radius)
              jiggle-radius)]
    {:x (+ x dx)
     :y (+ y dy)}))

(defn -squiggly-path [path & {:keys [seed]}]
  (random/seed (if seed
                 [seed path]
                 path))
  (let [points (-> path
                   path/new-path
                   (path/points :length))
        points (vec (concat [(first points)]
                            (map jiggle (partition 3 1 points))
                            [(last points)]))
        curve (catmullrom/catmullrom points)
        new-path (path/curve-to-relative curve)]
    new-path))

(def squiggly-path
  (memoize -squiggly-path))

(defn squiggly-paths [data]
  (walk/postwalk #(cond-> %
                    (vector? %) ((fn [v]
                                   (if (= (first v) :d)
                                     [:d (squiggly-path (second v))]
                                     v))))
                 data))
