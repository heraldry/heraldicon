(ns heraldry.coat-of-arms.catmullrom
  (:require [clojure.string :as string]))

;; catmullrom

(defn smooth-component [dir v0 v1 v2 tension]
  (dir v1 (* tension (/ (- v2 v0) 6))))

(defn smooth-point [dir p0 p1 p2 tension]
  (->> (range 2)
       (map (fn [i] (smooth-component dir (p0 i) (p1 i) (p2 i) tension)))
       (vec)))

(defn calculate-cubic-bezier-curve
  [tension [p0 p1 p2 p3]]
  (let [cp1 (smooth-point + p0 p1 p2 tension)
        cp2 (smooth-point - p1 p2 p3 tension)]
    [p1 cp1 cp2 p2]))

(defn catmullrom
  [points & {:keys [tension] :or {tension 1}}]
  (->> (concat [(first points)] points [(last points)])
       (map (juxt :x :y))
       (partition 4 1)
       (map (partial calculate-cubic-bezier-curve tension))))

;; svg

(defn svg-move-to [[x y]]
  (str "M" x "," y))

(defn svg-line-to [{:keys [x y]}]
  (str " l" x "," y " "))

(defn svg-curve-to-relative [[[px py] [cp1x cp1y] [cp2x cp2y] [p2x p2y]]]
  (str "c" (string/join "," (flatten [[(- cp1x px) (- cp1y py)] [(- cp2x px) (- cp2y py)] [(- p2x px) (- p2y py)]]))))

(defn curve->svg-path-relative [curve]
  (let [start (first (first curve))]
    (string/join "" (concat [(svg-move-to start)]
                            (map svg-curve-to-relative curve)))))

(defn square [x]
  (* x x))

(defn distance [{x0 :x y0 :y} {x1 :x y1 :y}]
  (Math/sqrt (+ (square (- x0 x1))
                (square (- y0 y1)))))

(defn bezier-length [[[x1 y1] _ _ [x2 y2]]]
  (distance {:x x1 :y y1} {:x x2 :y y2}))

(defn curve->length [path]
  (->> path
       (map bezier-length)
       (apply +)))

(defn interpolate-point-cubic [[p1 cp1 cp2 p2] t]
  (let [t2 (* t t)
        t3 (* t2 t)
        tr (- 1 t)
        tr2 (* tr tr)
        tr3 (* tr2 tr)]
    (->> (range 2)
         (map (fn [i] (+ (* tr3 (p1 i))
                         (* 3 tr2 t (cp1 i))
                         (* 3 tr t2 (cp2 i))
                         (* t3 (p2 i)))))
         (map vector [:x :y])
         (into {}))))

(defn close-to-zero? [value]
  (-> value
      Math/abs
      (< 0.000000000001)))

(defn calculate-tangent-points [[[p1x p1y] [c1x c1y] [c2x c2y] [p2x p2y]] [u v]]
  (let [;; shift them such that p1 = 0, the translation does not change the solutions
        [c d] [(- c1x p1x) (- c1y p1y)]
        [e f] [(- c2x p1x) (- c2y p1y)]
        [g h] [(- p2x p1x) (- p2y p1y)]
        determinant1 (-> (* h u)
                         (- (* g v))
                         (+ (* 3 v (- e c)))
                         (+ (* 3 u (- d f))))
        determinant4 (- (* 3 (- f d)) h)
        determinant5 (- (* 3 (- e c)) g)
        determinant6 (- (* f g u v)
                        (* e h u v))
        top-term (+ (* 3 e v) (* -3 f u) (* -1 g v) (* h u))
        bottom-term (* 2 (+ (* 3 e v) (* -3 f u) (* -2 g v) (* 2 h u)))]
    (->> [(when-not (close-to-zero? determinant1)
            ;; solutions here:
            ;; t1 = (-1/2 sqrt((4 c v - 4 d u - 2 e v + 2 f u)^2 - 4 (d u - c v) (-3 c v + 3 d u + 3 e v - 3 f u - g v + h u)) - 2 c v + 2 d u + e v - f u)/(-3 c v + 3 d u + 3 e v - 3 f u - g v + h u) and -3 c v + 3 d u + 3 e v - 3 f u - g v + h u!=0
            ;; t2 = ( 1/2 sqrt((4 c v - 4 d u - 2 e v + 2 f u)^2 - 4 (d u - c v) (-3 c v + 3 d u + 3 e v - 3 f u - g v + h u)) - 2 c v + 2 d u + e v - f u)/(-3 c v + 3 d u + 3 e v - 3 f u - g v + h u) and -3 c v + 3 d u + 3 e v - 3 f u - g v + h u!=0
            (let [determinant2 (-> 0
                                   (+ (* 2 v (- (* 2 c) e))
                                      (* 2 u (- f (* 2 d))))
                                   square
                                   (- (* 4 (- (* d u) (* c v)) determinant1)))]
              (when-not (neg? determinant2)
                (let [det-sqr (Math/sqrt determinant2)]
                  [(-> det-sqr
                       (/ -2)
                       (+ (* v (- e (* 2 c))))
                       (+ (* u (- (* 2 d) f)))
                       (/ determinant1))
                   (-> det-sqr
                       (/ 2)
                       (+ (* v (- e (* 2 c))))
                       (+ (* u (- (* 2 d) f)))
                       (/ determinant1))]))))
          (when (and (not (zero? v))
                     (close-to-zero? determinant1))
            ;; solutions here:
            ;; t3 = (3 e v - 3 f u - g v + h u)/(2 (3 e v - 3 f u - 2 g v + 2 h u)) and v!=0 and c = (3 d u + 3 e v - 3 f u - g v + h u)/(3 v) and -u (-9 d e v + 9 d f u + 6 d g v - 6 d h u + 9 e f v - 3 e h v - 9 f^2 u - 6 f g v + 9 f h u + 2 g h v - 2 h^2 u)!=0
            ;; simplified determinant (via Maple):
            ;; 9*((-f + (2*h)/3)*u + v*(e - (2*g)/3))*u*(d - f + h/3)
            ;; t4 = (-3 e v + 3 f u + g v - h u)/(2 (-3 e v + 3 f u + 2 g v - 2 h u)) and d = 1/3 (3 f - h) and c = 1/3 (3 e - g) and -3 e v + 3 f u + 2 g v - 2 h u!=0 and f g u v - e h u v!=0
            ;; same value, actually, but with different conditions
            (let [determinant3 (* 9
                                  u
                                  (+ (* (- (* h (/ 2 3)) f) u)
                                     (* (- e (* g (/ 2 3))) v))
                                  (+ d (- f) + (/ h 3)))]
              (when (or (and (not (close-to-zero? determinant3))
                             (not (close-to-zero? bottom-term)))
                        (and (close-to-zero? determinant4)
                             (close-to-zero? determinant5)
                             (not (close-to-zero? determinant6))
                             (not (close-to-zero? bottom-term))))
                [(/ top-term
                    bottom-term)])))
          (when (and (close-to-zero? u)
                     (not (close-to-zero? v))
                     (close-to-zero? determinant5))
            (let [divisor (* 2 (- (* 3 e) (* 2 g)))]
              (when-not (close-to-zero? divisor)
                [(/ (- (* 3 e) g)
                    divisor)])))]
         (apply concat)
         (filter (fn [t]
                   (and t
                        (<= 0 t 1))))
         sort)))

(defn split-bezier [[p1 c1 c2 p2] t]
  (let [n 3
        tr (- 1 t)]
    (loop [j 1
           matrix {[0 0] p1
                   [0 1] c1
                   [0 2] c2
                   [0 3] p2}]
      (if (> j n)
        {:curve1 [(get matrix [0 0])
                  (get matrix [1 0])
                  (get matrix [2 0])
                  (get matrix [3 0])]
         :curve2 [(get matrix [3 0])
                  (get matrix [2 1])
                  (get matrix [1 2])
                  (get matrix [0 3])]}
        (let [new-points (map (fn [i]
                                [[j i] (let [p1 (matrix [(dec j) i])
                                             p2 (matrix [(dec j) (inc i)])]
                                         (->> (range 2)
                                              (map (fn [comp]
                                                     (+ (* tr (p1 comp))
                                                        (* t (p2 comp)))))
                                              vec))])
                              (range (- (inc n) j)))]
          (recur (inc j)
                 (into matrix new-points)))))))

(defn split-curve-at [curve t]
  (let [total-length (curve->length curve)
        num-curves (count curve)
        absolute-t (* t total-length)
        [split-index
         rest-t
         _] (->> curve
                 (map bezier-length)
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
        rest-split (split-bezier (get curve split-index) rest-t)]
    {:curve1 (vec (concat (if (pos? split-index)
                            (subvec curve 0 split-index)
                            [])
                          [(:curve1 rest-split)]))
     :curve2 (vec (concat [(:curve2 rest-split)]
                          (if (< split-index (dec num-curves))
                            (subvec curve (inc split-index))
                            [])))}))

