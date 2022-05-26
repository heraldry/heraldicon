(ns heraldicon.svg.squiggly
  (:require
   [heraldicon.math.curve.catmullrom :as catmullrom]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]
   [heraldicon.util.random :as random]))

(defn- jiggle [[previous current _]]
  (let [dist (-> current
                 (v/sub previous)
                 (v/abs))
        jiggle-radius (/ dist 4)
        dx (- (* (random/float) jiggle-radius)
              jiggle-radius)
        dy (- (* (random/float) jiggle-radius)
              jiggle-radius)]
    (v/add current (v/Vector. dx dy))))

(def squiggly-path
  (memoize
   (fn squiggly-path [path & {:keys [seed]}]
     (random/seed (if seed
                    [seed path]
                    path))
     (let [points (-> path
                      path/parse-path
                      (path/points :length))
           points (vec (concat [(first points)]
                               (map jiggle (partition 3 1 points))
                               [(last points)]))
           curve (catmullrom/catmullrom points)
           new-path (path/curve-to-relative curve)]
       new-path))))
