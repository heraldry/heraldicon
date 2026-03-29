(ns heraldicon.heraldry.line.type.lilyous
  (:require
   [heraldicon.math.vector :as v]
   [heraldicon.util.core :as util]))

(defn- mirror-reverse-seg [[cp1x cp1y cp2x cp2y endx endy]]
  [(- endx cp2x) (- cp2y endy)
   (- endx cp1x) (- cp1y endy)
   endx (- endy)])

(defn- scale-seg [sx sy [cp1x cp1y cp2x cp2y ex ey]]
  ["c" (v/Vector. (* cp1x sx) (* cp1y sy))
   (v/Vector. (* cp2x sx) (* cp2y sy))
   (v/Vector. (* ex sx) (* ey sy))])

(defn- bezier-y-at-t [[_ c1y _ c2y _ ey] t]
  (let [s (- 1 t)]
    (+ (* 3 s s t c1y)
       (* 3 s t t c2y)
       (* t t t ey))))

(defn- split-bezier
  "Split a cubic bezier at parameter t using De Casteljau."
  [[c1x c1y c2x c2y ex ey] t]
  (let [s (- 1 t)
        m01x (* c1x t) m01y (* c1y t)
        m12x (+ (* c1x s) (* c2x t)) m12y (+ (* c1y s) (* c2y t))
        m23x (+ (* c2x s) (* ex t)) m23y (+ (* c2y s) (* ey t))
        m012x (+ (* m01x s) (* m12x t)) m012y (+ (* m01y s) (* m12y t))
        m123x (+ (* m12x s) (* m23x t)) m123y (+ (* m12y s) (* m23y t))
        mx (+ (* m012x s) (* m123x t)) my (+ (* m012y s) (* m123y t))]
    [[m01x m01y m012x m012y mx my]
     [(- m123x mx) (- m123y my)
      (- m23x mx) (- m23y my)
      (- ex mx) (- ey my)]]))

(defn- find-t-for-y
  "Find t where bezier y(t) = target-y using bisection."
  [seg target-y]
  (let [lo-y (bezier-y-at-t seg 0)]
    (loop [lo 0.0 hi 1.0 n 0]
      (if (> n 100)
        (/ (+ lo hi) 2)
        (let [mid (/ (+ lo hi) 2)
              y (bezier-y-at-t seg mid)]
          (if (< (js/Math.abs (- y target-y)) 0.0001)
            mid
            (if (neg? (* (- y target-y) (- lo-y target-y)))
              (recur lo mid (inc n))
              (recur mid hi (inc n)))))))))

(def ^:private ascending-segments
  [[22.1196 -11.3032 26.6074 -47.6093 14.578 -74.0603]
   [19.5619 14.0182 42.2343 -0.710363 36.5723 -23.33]
   [-9.21572 -1.53581 -6.31729 8.8903 -16.9798 9.16753]
   [-10.6625 0.277228 -18.6866 -7.04013 -18.7186 -16.1796]
   [-0.0337323 -9.63525 5.70133 -17.7551 15.9055 -17.655]
   [10.2042 0.100063 10.3068 10.7218 20.0619 7.13934]
   [-2.42164 -29.8817 -28.0276 -28.1795 -39.2465 -15.7853]
   [-3.50759 -14.5599 7.00101 -24.9491 19.9103 -25.0268]
   [12.9093 -0.0776693 15.4151 11.6376 20.9401 10.4108]
   [-16.6915 -32.4043 -14.1044 -60.2799 10.7201 -83.5495]])

(def ^:private descending-segments
  (mapv mirror-reverse-seg (rseq ascending-segments)))

(def ^:private ascending-total-y
  (reduce + (map #(nth % 5) ascending-segments)))

(def ^:private target-y (* ascending-total-y 0.468))

(def ^:private split-info
  (loop [i 0 cum-y 0]
    (let [seg (nth ascending-segments i)
          next-cum (+ cum-y (nth seg 5))]
      (if (or (= i 9)
              (and (<= next-cum target-y) (>= cum-y target-y))
              (and (>= next-cum target-y) (<= cum-y target-y)))
        (let [local-target (- target-y cum-y)
              t (find-t-for-y seg local-target)]
          {:index i :t t})
        (recur (inc i) next-cum)))))

;; Forward pattern: split ascending at the computed point
(def ^:private fwd-split-halves
  (split-bezier (nth ascending-segments (:index split-info)) (:t split-info)))

(def ^:private ordered-segments
  (let [idx (:index split-info)]
    (vec (concat
          [(second fwd-split-halves)]
          (subvec ascending-segments (inc idx))
          descending-segments
          (subvec ascending-segments 0 idx)
          [(first fwd-split-halves)]))))

;; Mirror pattern: split the corresponding descending segment at (1-t)
;; so that after automatic reverse+mirror, features align with forward
(def ^:private mirror-desc-index
  (- 9 (:index split-info)))

(def ^:private mirror-split-halves
  (split-bezier (nth descending-segments mirror-desc-index)
                (- 1 (:t split-info))))

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

(def pattern
  {:display-name :string.line.type/lilyous
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
