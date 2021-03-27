(ns heraldry.coat-of-arms.vector
  (:require ["path-intersection" :as path-intersection]))

(defn v [x y]
  {:x x
   :y y})

(def zero
  (v 0 0))

#_{:clj-kondo/ignore [:redefined-var]}
(defn + [& args]
  {:x (apply cljs.core/+ (map :x args))
   :y (apply cljs.core/+ (map :y args))})

#_{:clj-kondo/ignore [:redefined-var]}
(defn - [& args]
  {:x (apply cljs.core/- (map :x args))
   :y (apply cljs.core/- (map :y args))})

#_{:clj-kondo/ignore [:redefined-var]}
(defn * [{x :x y :y} f]
  {:x (cljs.core/* x f)
   :y (cljs.core/* y f)})

#_{:clj-kondo/ignore [:redefined-var]}
(defn / [{x :x y :y} f]
  {:x (cljs.core// x f)
   :y (cljs.core// y f)})

(defn dot [{x1 :x y1 :y} {x2 :x y2 :y}]
  {:x (cljs.core/* x1 x2)
   :y (cljs.core/* y1 y2)})

(defn abs [{x :x y :y}]
  (Math/sqrt (cljs.core/+
              (cljs.core/* x x)
              (cljs.core/* y y))))

(defn normal [v]
  (let [d (abs v)]
    (if (> d 0)
      (/ v d)
      v)))

(defn avg [v1 v2]
  (-> v1
      (+ v2)
      (/ 2)))

(defn div-x [{x :x :as p}]
  (/ p (Math/abs x)))

(defn project [{from-x :x from-y :y} {to-x :x to-y :y} x]
  {:x x
   :y (-> to-y
          (cljs.core/- from-y)
          (cljs.core// (cljs.core/- to-x from-x))
          (cljs.core/* (cljs.core/- x from-x))
          (cljs.core/+ from-y))})

(defn project-x [{from-x :x from-y :y} {dir-x :x dir-y :y} x]
  {:x x
   :y (-> dir-y
          (cljs.core// dir-x)
          (cljs.core/* (cljs.core/- x from-x))
          (cljs.core/+ from-y))})

(defn extend [from to l]
  (let [diff      (- to from)
        distance  (abs diff)
        direction (/ diff distance)]
    (-> direction
        (* l)
        (+ from))))

(defn rotate [{:keys [x y]} angle]
  (let [rad (-> angle
                (clojure.core/* Math/PI)
                (clojure.core// 180))]
    (v (clojure.core/- (clojure.core/* x (Math/cos rad))
                       (clojure.core/* y (Math/sin rad)))
       (clojure.core/+ (clojure.core/* x (Math/sin rad))
                       (clojure.core/* y (Math/cos rad))))))

(defn distance-point-to-line [{x0 :x y0 :y} {x1 :x y1 :y :as p1} {x2 :x y2 :y :as p2}]
  (cljs.core// (Math/abs (cljs.core/- (cljs.core/* (cljs.core/- x2 x1)
                                                   (cljs.core/- y1 y0))
                                      (cljs.core/* (cljs.core/- x1 x0)
                                                   (cljs.core/- y2 y1))))
               (abs (- p1 p2))))

(defn line-intersection [{x1 :x y1 :y}
                         {x2 :x y2 :y :as end1}
                         {x3 :x y3 :y :as start2}
                         {x4 :x y4 :y}]
  (let [- cljs.core/-
        * cljs.core/*
        D (- (* (- x1 x2)
                (- y3 y4))
             (* (- y1 y2)
                (- x3 x4)))]
    (if (zero? D)
      ;; not expected, but if D = 0, then the lines are parallel,
      ;; in that case just take the middle of end1 and start2
      (-> end1
          (+ start2)
          (/ 2))
      (/ (v (-> (* (- (* x1 y2)
                      (* y1 x2))
                   (- x3 x4))
                (- (* (- x1 x2)
                      (- (* x3 y4)
                         (* y3 x4)))))
            (-> (* (- (* x1 y2)
                      (* y1 x2))
                   (- y3 y4))
                (- (* (- y1 y2)
                      (- (* x3 y4)
                         (* y3 x4))))))
         D))))

(defn tangent-point [{cx :x cy :y} r {px :x py :y}]
  (let [+          cljs.core/+
        -          cljs.core/-
        *          cljs.core/*
        /          cljs.core//
        dx         (- px cx)
        dy         (- py cy)
        r2         (* r r)
        dx2        (* dx dx)
        dy2        (* dy dy)
        sum-d2     (+ dx2 dy2)
        D          (- sum-d2 r2)
        dir-factor 1]
    (when (>= D 0)
      (let [sqrtD (Math/sqrt D)]
        (v (-> (+ (* r2 dx) (* dir-factor r dy sqrtD))
               (/ sum-d2)
               (+ cx))
           (-> (- (* r2 dy) (* dir-factor r dx sqrtD))
               (/ sum-d2)
               (+ cy)))))))

(defn outer-tangent-between-circles [{x0 :x y0 :y} r0
                                     {x1 :x y1 :y} r1
                                     edge]
  (let [+          cljs.core/+
        -          cljs.core/-
        *          cljs.core/*
        /          cljs.core//
        dir-factor (if (= edge :left)
                     1
                     -1)
        dx         (- x1 x0)
        dy         (- y1 y0)
        dist       (Math/sqrt (+ (* dx dx) (* dy dy)))
        gamma      (- (Math/atan2 dy dx))
        beta       (-> (- r1 r0)
                       (/ dist)
                       Math/asin
                       (* dir-factor))
        alpha      (- gamma beta)]
    [(v (+ x0 (* dir-factor r0 (Math/sin alpha)))
        (+ y0 (* dir-factor r0 (Math/cos alpha))))
     (v (+ x1 (* dir-factor r1 (Math/sin alpha)))
        (+ y1 (* dir-factor r1 (Math/cos alpha))))]))

(defn inner-tangent-between-circles [{x0 :x y0 :y} r0
                                     {x1 :x y1 :y} r1
                                     edge]
  (let [+          cljs.core/+
        -          cljs.core/-
        *          cljs.core/*
        /          cljs.core//
        dir-factor (if (= edge :left)
                     1
                     -1)
        dx         (- x1 x0)
        dy         (- y1 y0)
        dist       (Math/sqrt (+ (* dx dx) (* dy dy)))
        gamma      (- (Math/atan2 dy dx))
        beta       (-> (+ r0 r1)
                       (/ dist)
                       Math/asin
                       (* dir-factor))
        alpha      (- gamma beta)]
    [(v (- x0 (* dir-factor r0 (Math/sin alpha)))
        (- y0 (* dir-factor r0 (Math/cos alpha))))
     (v (+ x1 (* dir-factor r1 (Math/sin alpha)))
        (+ y1 (* dir-factor r1 (Math/cos alpha))))]))

(defn orthogonal [{:keys [x y]}]
  (v y (cljs.core/- x)))

(defn ->str [{:keys [x y]}]
  (str x "," y))

(defn prune-duplicates [intersections]
  (->> intersections
       (group-by (juxt :x :y :parent-index))
       (map (comp first second))))

(defn find-intersections [from to environment]
  (let [shapes           (->> environment
                              (tree-seq map? (comp list :parent-environment :meta))
                              (map :shape)
                              (filter identity))
        line-path        (str "M" (->str from)
                              "L" (->str to))
        intersections    (->> shapes
                              (map-indexed (fn [parent-idx shape]
                                             (-> (path-intersection line-path shape)
                                                 (js->clj :keywordize-keys true)
                                                 (->> (map #(assoc % :parent-index parent-idx))))))
                              (apply concat)
                              vec)]
    (->> intersections
         prune-duplicates
         (sort-by :t1))))

(defn find-first-intersection-of-ray [origin anchor environment]
  (let [direction-vector (- anchor origin)
        line-end         (-> direction-vector
                             normal
                             (* 1000)
                             (+ origin))
        intersections    (find-intersections origin line-end environment)]
    (-> intersections
        (->> (filter (comp pos? :t1)))
        first
        (select-keys [:x :y]))))

(defn angle-to-point [p1 p2]
  (let [d         (- p2 p1)
        angle-rad (Math/atan2 (:y d) (:x d))]
    (-> angle-rad
        (cljs.core// Math/PI)
        (cljs.core/* 180))))

(defn bounding-box-intersections [from to environment]
  (let [{:keys [top-left top-right
                bottom-left bottom-right]} (:points environment)
        line-path        (str "M" (->str from)
                              "L" (->str to))
        box-shape (str "M" (->str top-left)
                       "L" (->str top-right)
                       "L" (->str bottom-right)
                       "L" (->str bottom-left)
                       "z")]
    (-> (path-intersection line-path box-shape)
        (js->clj :keywordize-keys true)
        prune-duplicates
        (->> (sort-by :t1)))))

(defn environment-intersections [from to environment]
  (let [[bbox-first bbox-second] (bounding-box-intersections from to environment)
        middle (-> (+ bbox-first bbox-second)
                   (/ 2))]
    [(find-first-intersection-of-ray middle from environment)
     (find-first-intersection-of-ray middle to environment)]))

