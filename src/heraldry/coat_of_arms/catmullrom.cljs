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

(defn svg-curve-to-relative [[[px py] [cp1x cp1y] [cp2x cp2y] [p2x p2y]]]
  (str "c" (string/join "," (flatten [[(- cp1x px) (- cp1y py)] [(- cp2x px) (- cp2y py)] [(- p2x px) (- p2y py)]]))))

(defn curve->svg-path-relative [curve]
  (let [start (first (first curve))]
    (string/join "" (concat [(svg-move-to start)]
                            (map svg-curve-to-relative curve)))))

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
      (< 0.0000001)))

(defn calculate-tangent-points [bezier-points x]
  ;; x is the desired slope of the tangent
  ;; WolframAlpha:
  ;; solve(derive(3*(1-t)^2*t*c1 + 3*(1-t)*t^2*c2+t^3*p2, t) = x, t)
  (let [;; only care about the y-components here
        [p1 c1 c2 p2] (map second bezier-points)
        ;; shift them such that p1 = 0, the translation not changing the solutions
        c1 (- c1 p1)
        c2 (- c2 p1)
        p2 (- p2 p1)
        determinant1 (* 3 (+ p2 (* 3 (- c1 c2))))]
    (if (close-to-zero? determinant1)
      ;; the two "usual" solutions are out, check for a third one
      (let [determinant2 (* 2 (- (* 3 c2) (* 2 p2)))]
        (if (close-to-zero? determinant2)
          ;; no solutions in that case
          []
          ;; one possible solution, if it is in the range
          [(/ (- 3 c2 p2 x) determinant2)]))
      (let [determinant2 (-> 0
                             (+ (* 9 c1 c1))
                             (+ (* 9 c2 c2))
                             (+ (* 3 p2 x))
                             (- (* 9 c1 (+ c2 p2 (- x))))
                             (- (* 9 c2 x)))]
        (if (neg? determinant2)
          ;; no real solutions
          []
          ;; otherwise two possible solutions
          [(+ (- (Math/sqrt determinant2))
              (* 6 c1)
              (* -3 c2))
           (+ (Math/sqrt determinant2)
              (* 6 c1)
              (* -3 c2))])))))

