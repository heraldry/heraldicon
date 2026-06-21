(ns heraldicon.math.curve.catmullrom
  (:require
   [heraldicon.math.vector :as v]))

(defn catmullrom [points & {:keys [^js/Number tension closed?] :or {tension 1}}]
  ;; Produces one cubic bezier segment per consecutive pair of points.
  ;; Open (default): the first and last points are used as ghost control points
  ;; (clamped ends), matching the behaviour of the original partition-4-1
  ;; approach. Closed?: the point list is treated as a periodic loop — neighbour
  ;; lookups wrap around and an extra segment connects the last point back to the
  ;; first, so the seam where a closed contour's end meets its start stays smooth.
  (let [pts (if (vector? points) points (vec points))
        n (count pts)]
    (when (> n 1)
      (let [p0 (nth pts 0)
            pn (nth pts (dec n))
            t6 (/ tension 6)
            ;; closed loops emit one segment per point (wrapping n-1 -> 0);
            ;; open paths emit one per consecutive pair (0..n-2).
            seg-count (if closed? n (dec n))]
        ;; i is the index of the START point of each segment (p1 in CR terminology).
        (loop [i 0
               acc (transient [])]
          (if (< i seg-count)
            (let [;; four-point window: pa p1 p2 pd
                  ;; pa = point before p1 (wraps when closed, ghost = p0 when open+i=0)
                  pa (cond
                       closed? (nth pts (mod (dec i) n))
                       (zero? i) p0
                       :else (nth pts (dec i)))
                  pb (nth pts i)
                  pc (nth pts (mod (inc i) n))
                  ;; pd = point after p2 (wraps when closed, ghost = pn when open+i=n-2)
                  pd (cond
                       closed? (nth pts (mod (+ i 2) n))
                       (< (+ i 2) n) (nth pts (+ i 2))
                       :else pn)
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
