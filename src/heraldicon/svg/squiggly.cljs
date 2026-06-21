(ns heraldicon.svg.squiggly
  (:require
   [heraldicon.math.curve.catmullrom :as catmullrom]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]
   [heraldicon.util.cache :as cache]
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

(defn- closed-path?
  "Is this contour a closed loop? Paper only flags `.closed` when the path data
  actually carries a `z`, but several of our producers (round-corners,
  modify-path, ...) rebuild a loop via curve-to-relative without one. So also
  treat it as closed when the first and last sampled points coincide — within
  half a sample step, which is tiny for a loop (endpoints ~equal) but huge for
  an open line segment (endpoints a whole length apart)."
  [^js/Object parsed-path points]
  (let [n (count points)]
    (or (.-closed parsed-path)
        (and (> n 1)
             (let [step (/ (path/length parsed-path) (dec n))]
               (< (v/abs (v/sub (first points) (last points)))
                  (* 0.5 step)))))))

(defn- jiggle-open
  "Open path: keep the first and last points fixed so the segment still meets
  its neighbours, jiggle everything in between."
  [points]
  (vec (concat [(first points)]
               (map jiggle (partition 3 1 points))
               [(last points)])))

(defn- jiggle-closed
  "Closed contour: there are no endpoints to anchor, so jiggle every point using
  its wrapped neighbours. The sampler always emits a final point at offset =
  length, i.e. coinciding with the start, so we drop it to avoid doubling the
  seam vertex before the periodic catmullrom wraps the loop."
  [points]
  (let [points (cond-> (vec points)
                 (> (count points) 1) pop)
        n (count points)]
    (mapv (fn [i]
            (jiggle [(nth points (mod (dec i) n))
                     (nth points i)
                     (nth points (mod (inc i) n))]))
          (range n))))

(def squiggly-path
  (cache/memoize
   ::squiggly-path
   (fn squiggly-path [path & {:keys [seed]}]
     (random/seed (if seed
                    [seed path]
                    path))
     (let [parsed-path (path/parse-path path)
           points (path/points parsed-path :length)]
       (if (and (closed-path? parsed-path points)
                (> (count points) 3))
         ;; periodic catmullrom + explicit close keeps the start/end junction
         ;; of a closed contour continuous instead of leaving a flat/kinked seam
         (-> (jiggle-closed points)
             (catmullrom/catmullrom :closed? true)
             path/curve-to-relative
             (str "z"))
         (-> (jiggle-open points)
             catmullrom/catmullrom
             path/curve-to-relative))))))
