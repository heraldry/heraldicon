(ns heraldicon.heraldry.line.type.spaded
  (:require
   [heraldicon.math.vector :as v]
   [heraldicon.util.core :as util]))

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

(def ^:private descending-segments
  (mapv mirror-reverse-seg (rseq ascending-segments)))

(def ^:private fwd-split-halves
  (split-bezier (nth ascending-segments 2)))

;; Start at the vertical midpoint (t=0.5 of s3), so the pattern is
;; centered at y=0.
(def ^:private ordered-segments
  (vec (concat
        [(second fwd-split-halves)]
        (subvec ascending-segments 3)
        descending-segments
        (subvec ascending-segments 0 2)
        [(first fwd-split-halves)])))

;; Mirror pattern: split the corresponding descending segment at (1-t)
;; so that after automatic reverse+mirror, features align with forward
(def ^:private mirror-desc-index
  (- 4 2))

(def ^:private mirror-split-halves
  (split-bezier (nth descending-segments mirror-desc-index)))

(def ^:private mirrored-ordered-segments
  (let [didx mirror-desc-index]
    (vec (concat
          [(second mirror-split-halves)]
          (subvec descending-segments (inc didx))
          ascending-segments
          (subvec descending-segments 0 didx)
          [(first mirror-split-halves)]))))

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
   :function (fn [{line-mirrored? :mirrored?
                   :keys [height width]}
                  {:keys [reversed? mirrored?]}]
               (let [effective-mirrored? (-> (boolean line-mirrored?)
                                             (util/xor (boolean mirrored?))
                                             (util/xor (boolean reversed?)))
                     sx (/ width period-width)
                     sy (* sx height)
                     segs (if effective-mirrored?
                            mirrored-ordered-segments
                            ordered-segments)
                     scaled (mapcat (partial scale-seg sx sy) segs)]
                 {:pattern (vec scaled)
                  :min (* tip-y sy)
                  :max (* valley-y sy)}))})
