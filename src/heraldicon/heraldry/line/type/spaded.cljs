(ns heraldicon.heraldry.line.type.spaded
  (:require
   [heraldicon.math.vector :as v]))

(defn- mirror-reverse-seg [[cp1x cp1y cp2x cp2y endx endy]]
  [(- endx cp2x) (- cp2y endy)
   (- endx cp1x) (- cp1y endy)
   endx (- endy)])

(defn- flip-mirror-seg
  "Transform that ensures overlap when y-flipped and offset by half a period."
  [[c1x c1y c2x c2y ex ey]]
  [(- ex c2x) (- ey c2y)
   (- ex c1x) (- ey c1y)
   ex ey])

(defn- split-bezier
  "Split a cubic bezier at t=0.5 using De Casteljau. Returns [first-half second-half]."
  [[c1x c1y c2x c2y ex ey]]
  (let [m01x (/ c1x 2) m01y (/ c1y 2)
        m12x (/ (+ c1x c2x) 2) m12y (/ (+ c1y c2y) 2)
        m23x (/ (+ c2x ex) 2) m23y (/ (+ c2y ey) 2)
        m012x (/ (+ m01x m12x) 2) m012y (/ (+ m01y m12y) 2)
        m123x (/ (+ m12x m23x) 2) m123y (/ (+ m12y m23y) 2)
        mx (/ (+ m012x m123x) 2) my (/ (+ m012y m123y) 2)]
    [[m01x m01y m012x m012y mx my]
     [(- m123x mx) (- m123y my)
      (- m23x mx) (- m23y my)
      (- ex mx) (- ey my)]]))

(def ^:private base-segments
  (let [s1 [17.72 -19.28 39.56 -39.48 39.27 -47.25]
        s2 [-0.33 -8.79 -18.21 -21.12 -30.28 -10.08]
        s3 [-0.79 -20.675 36.23 -23.435 35.44 -44.11]]
    [s1 s2 s3]))

(def ^:private ascending-segments
  (let [s4 (flip-mirror-seg (nth base-segments 1))
        s5 (flip-mirror-seg (nth base-segments 0))]
    (vec (concat base-segments [s4 s5]))))

(def ^:private s3-halves
  (split-bezier (nth base-segments 2)))

(def ^:private s3-first (first s3-halves))

(def ^:private s3-second (second s3-halves))

;; Start at the vertical midpoint (t=0.5 of s3), so the pattern is
;; centered at y=0. The flip-mirror symmetry ensures opposite line alignment.
(def ^:private ordered-segments
  (vec (concat
        ;; ascending: from midpoint to tip
        [s3-second]
        [(flip-mirror-seg (nth base-segments 1))
         (flip-mirror-seg (nth base-segments 0))]
        ;; descending: full mirror of ascending
        (mapv mirror-reverse-seg (rseq ascending-segments))
        ;; ascending: from valley to midpoint
        (subvec ascending-segments 0 2)
        [s3-first])))

(def ^:private period-width
  (* 2 (reduce + (map #(nth % 4) ascending-segments))))

(def ^:private cumulative-y
  (reductions + 0 (map #(nth % 5) ordered-segments)))

(def ^:private tip-y (apply min cumulative-y))

(def ^:private valley-y (apply max cumulative-y))

(defn- scale-seg [sx sy [cp1x cp1y cp2x cp2y ex ey]]
  ["c" (v/Vector. (* cp1x sx) (* cp1y sy))
   (v/Vector. (* cp2x sx) (* cp2y sy))
   (v/Vector. (* ex sx) (* ey sy))])

(def pattern
  {:display-name :string.line.type/spaded
   :function (fn [{:keys [height width]}
                  _line-options]
               (let [sx (/ width period-width)
                     sy (* sx height)
                     scaled (mapcat (partial scale-seg sx sy)
                                    ordered-segments)]
                 {:pattern (vec scaled)
                  :min (* tip-y sy)
                  :max (* valley-y sy)}))})
