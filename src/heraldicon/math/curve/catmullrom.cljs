(ns heraldicon.math.curve.catmullrom
  (:require
   [heraldicon.math.vector :as v]))

(defn catmullrom [points & {:keys [^js/Number tension] :or {tension 1}}]
  ;; Produces one cubic bezier segment per consecutive pair of points.
  ;; The first and last points are used as ghost control points (clamped ends),
  ;; matching the behaviour of the original partition-4-1 approach.
  (let [pts (if (vector? points) points (vec points))
        n (count pts)]
    (when (> n 1)
      (let [p0 (nth pts 0)
            pn (nth pts (dec n))
            t6 (/ tension 6)]
        ;; i is the index of the START point of each segment (p1 in CR terminology).
        ;; Segment i goes from pts[i] to pts[i+1], so i runs 0..(n-2).
        (loop [i 0
               acc (transient [])]
          (if (< i (dec n))
            (let [;; four-point window: pa p1 p2 pd
                  ;; pa = point before p1 (ghost = p0 when i=0)
                  pa (if (zero? i) p0 (nth pts (dec i)))
                  pb (nth pts i)
                  pc (nth pts (inc i))
                  ;; pd = point after p2 (ghost = pn when i = n-2)
                  pd (if (< (+ i 2) n) (nth pts (+ i 2)) pn)
                  {xa :x ya :y} pa
                  {xb :x yb :y} pb
                  {xc :x yc :y} pc
                  {xd :x yd :y} pd]
              (recur (inc i)
                     (conj! acc
                            [pb
                             (v/Vector. (+ xb (* t6 (- xc xa)))
                                        (+ yb (* t6 (- yc ya))))
                             (v/Vector. (- xc (* t6 (- xd xb)))
                                        (- yc (* t6 (- yd yb))))
                             pc])))
            (persistent! acc)))))))
