(ns heraldicon.math.curve.bezier
  (:require
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]))

(defn- square ^js/Number [^js/Number x]
  (* x x))

(defn length ^js/Number [[^v/Vector p1 _ _ ^v/Vector p2]]
  ;; TODO: this is inaccurate and fine for now, but maybe the real value would be nice
  (v/abs (v/sub p1 p2)))

(defn interpolate-point ^v/Vector [[^v/Vector p1
                                    ^v/Vector cp1
                                    ^v/Vector cp2
                                    ^v/Vector p2]
                                   ^js/Number t]
  (let [t2 (* t t)
        t3 (* t2 t)
        tr (- 1 t)
        tr2 (* tr tr)
        tr3 (* tr2 tr)]
    (-> (v/mul p1 tr3)
        (v/add (v/mul cp1 (* 3 tr2 t)))
        (v/add (v/mul cp2 (* 3 tr t2)))
        (v/add (v/mul p2 t3)))))

(defn calculate-tangent-points [[^v/Vector p1
                                 ^v/Vector cp1
                                 ^v/Vector cp2
                                 ^v/Vector p2]
                                ^v/Vector slope]
  (let [;; shift them such that p1 = 0, the translation does not change the solutions
        {c :x
         d :y} (v/sub cp1 p1)
        {e :x
         f :y} (v/sub cp2 p1)
        {g :x
         h :y} (v/sub p2 p1)
        {u :x
         v :y} slope
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
    (->> [(when-not (math/close-to-zero? determinant1)
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
                     (math/close-to-zero? determinant1))
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
              (when (or (and (not (math/close-to-zero? determinant3))
                             (not (math/close-to-zero? bottom-term)))
                        (and (math/close-to-zero? determinant4)
                             (math/close-to-zero? determinant5)
                             (not (math/close-to-zero? determinant6))
                             (not (math/close-to-zero? bottom-term))))
                [(/ top-term
                    bottom-term)])))
          (when (and (math/close-to-zero? u)
                     (not (math/close-to-zero? v))
                     (math/close-to-zero? determinant5))
            (let [divisor (* 2 (- (* 3 e) (* 2 g)))]
              (when-not (math/close-to-zero? divisor)
                [(/ (- (* 3 e) g)
                    divisor)])))]
         (apply concat)
         (filter (fn [t]
                   (and t
                        (<= 0 t 1))))
         sort)))

(defn split [[^v/Vector p1
              ^v/Vector c1
              ^v/Vector c2
              ^v/Vector p2]
             ^js/Number t]
  (let [n 3
        tr (- 1 t)]
    (loop [j 1
           matrix {[0 0] p1
                   [0 1] c1
                   [0 2] c2
                   [0 3] p2}]
      (if (> j n)
        {:bezier1 [(get matrix [0 0])
                   (get matrix [1 0])
                   (get matrix [2 0])
                   (get matrix [3 0])]
         :bezier2 [(get matrix [3 0])
                   (get matrix [2 1])
                   (get matrix [1 2])
                   (get matrix [0 3])]}
        (let [new-points (map (fn [i]
                                [[j i] (let [p1 (matrix [(dec j) i])
                                             p2 (matrix [(dec j) (inc i)])]
                                         (v/add (v/mul p1 tr)
                                                (v/mul p2 t)))])
                              (range (- (inc n) j)))]
          (recur (inc j)
                 (into matrix new-points)))))))
