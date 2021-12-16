(ns heraldry.math.vector
  (:require
   ["paper" :refer [Path]]
   [heraldry.math.core :as math]))

(defn v [x y]
  {:x x
   :y y})

(def zero
  (v 0 0))

(defn add [& args]
  {:x (apply + (map :x args))
   :y (apply + (map :y args))})

(defn sub [& args]
  {:x (apply - (map :x args))
   :y (apply - (map :y args))})

(defn mul [{x :x y :y} f & args]
  {:x (apply * (concat [x f] args))
   :y (apply * (concat [y f] args))})

(defn div [{x :x y :y} f & args]
  {:x (apply / (concat [x f] args))
   :y (apply / (concat [y f] args))})

(defn dot [{x1 :x y1 :y} {x2 :x y2 :y}]
  {:x (* x1 x2)
   :y (* y1 y2)})

(defn abs [{x :x y :y}]
  (Math/sqrt (+
              (* x x)
              (* y y))))

(defn normal [v]
  (let [d (abs v)]
    (if (> d 0)
      (div v d)
      v)))

(defn avg [v1 v2]
  (-> v1
      (add v2)
      (div 2)))

(defn rotate [{:keys [x y]} angle]
  (let [rad (math/to-rad angle)]
    (v (- (* x (Math/cos rad))
          (* y (Math/sin rad)))
       (+ (* x (Math/sin rad))
          (* y (Math/cos rad))))))

(defn distance-point-to-line [{x0 :x y0 :y} {x1 :x y1 :y :as p1} {x2 :x y2 :y :as p2}]
  (/ (Math/abs (- (* (- x2 x1)
                     (- y1 y0))
                  (* (- x1 x0)
                     (- y2 y1))))
     (abs (sub p1 p2))))

(defn line-intersection [{x1 :x y1 :y}
                         {x2 :x y2 :y :as end1}
                         {x3 :x y3 :y :as start2}
                         {x4 :x y4 :y}]
  (let [D (- (* (- x1 x2)
                (- y3 y4))
             (* (- y1 y2)
                (- x3 x4)))]
    (if (zero? D)
      ;; not expected, but if D = 0, then the lines are parallel,
      ;; in that case just take the middle of end1 and start2
      (-> end1
          (add start2)
          (div 2))
      (div (v (-> (* (- (* x1 y2)
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
  (let [dx (- px cx)
        dy (- py cy)
        r2 (* r r)
        dx2 (* dx dx)
        dy2 (* dy dy)
        sum-d2 (+ dx2 dy2)
        D (- sum-d2 r2)
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
  (let [dir-factor (if (= edge :left)
                     1
                     -1)
        dx (- x1 x0)
        dy (- y1 y0)
        dist (Math/sqrt (+ (* dx dx) (* dy dy)))
        gamma (- (Math/atan2 dy dx))
        beta (-> (- r1 r0)
                 (/ dist)
                 Math/asin
                 (* dir-factor))
        alpha (- gamma beta)]
    [(v (+ x0 (* dir-factor r0 (Math/sin alpha)))
        (+ y0 (* dir-factor r0 (Math/cos alpha))))
     (v (+ x1 (* dir-factor r1 (Math/sin alpha)))
        (+ y1 (* dir-factor r1 (Math/cos alpha))))]))

(defn inner-tangent-between-circles [{x0 :x y0 :y} r0
                                     {x1 :x y1 :y} r1
                                     edge]
  (let [dir-factor (if (= edge :left)
                     1
                     -1)
        dx (- x1 x0)
        dy (- y1 y0)
        dist (Math/sqrt (+ (* dx dx) (* dy dy)))
        gamma (- (Math/atan2 dy dx))
        beta (-> (+ r0 r1)
                 (/ dist)
                 Math/asin
                 (* dir-factor))
        alpha (- gamma beta)]
    [(v (- x0 (* dir-factor r0 (Math/sin alpha)))
        (- y0 (* dir-factor r0 (Math/cos alpha))))
     (v (+ x1 (* dir-factor r1 (Math/sin alpha)))
        (+ y1 (* dir-factor r1 (Math/cos alpha))))]))

(defn orthogonal [{:keys [x y]}]
  (v y (- x)))

(defn ->str [{:keys [x y]}]
  (str x "," y))

(defn prune-duplicates [intersections]
  (->> intersections
       (group-by (juxt :x :y))
       (map (comp first second))))

(defn prune-point [intersections point]
  (->> intersections
       (filter #(not= (select-keys % [:x :y])
                      (select-keys point [:x :y])))))

(defn -path-intersection [path1 path2]
  (let [p1 (new Path path1)
        p2 (new Path path2)]
    (into []
          (map (fn [^js/Object location]
                 {:x (.. location -point -x)
                  :y (.. location -point -y)
                  :t1 (/ (.. location -offset) (.-length p1))
                  :t2 (/ (.. location -intersection -offset) (.-length p2))}))
          (.getIntersections p1 p2))))

(def path-intersection
  (memoize -path-intersection))

(defn inside-shape? [point shape]
  (let [ray (str "M" (->str point) "l" 10000 "," 9000)
        intersections (into []
                            (map #(-> (path-intersection ray %)
                                      prune-duplicates
                                      (prune-point point)))
                            (:paths shape))]
    (-> intersections
        count
        odd?)))

(defn close-to-single-path? [point path]
  (let [radius 0.0001
        left (sub point (v radius 0))
        right (add point (v radius 0))
        neighbourhood (str "M" (->str left)
                           "A" radius " " radius " 0 0 0 " (->str right)
                           "A" radius " " radius " 0 0 1 " (->str left))
        intersections (path-intersection neighbourhood path)]
    (-> intersections count pos?)))

(defn close-to-edge? [point {:keys [paths] :as _shape}]
  (->> paths
       (map (partial close-to-single-path? point))
       (some true?)))

(defn inside-environment? [point environment]
  (->> environment
       (tree-seq map? (comp list :parent-environment :meta))
       (map :shape)
       (filter identity)
       (map-indexed (fn [parent-idx shape]
                      ;; there's some inaccuracy, where a point that previously was found
                      ;; as intersection won't be considered inside a shape when tested on
                      ;; its own, so for those cases check whether it was detected as an
                      ;; intersection for this parent index
                      ;; for other cases check whether a neighbourhood around the point
                      ;; intersects with the shape, then we consider the point close enough
                      ;; and "on" the edge
                      (or (-> point :parent-index (= parent-idx))
                          (close-to-edge? point shape)
                          (inside-shape? point shape))))
       (every? true?)))

(defn find-intersections [from to environment]
  (let [shapes (->> environment
                    (tree-seq map? (comp list :parent-environment :meta))
                    (map :shape)
                    (filter identity))
        line-path (str "M" (->str from)
                       "L" (->str to))
        intersections (->> shapes
                           (map-indexed (fn [parent-idx shape]
                                          (into []
                                                (mapcat (fn [path]
                                                          (->> (path-intersection line-path path)
                                                               (map #(assoc % :parent-index parent-idx)))))
                                                (:paths shape))))
                           (apply concat)
                           (filter #(inside-environment? % environment))
                           vec)]
    (->> intersections
         prune-duplicates
         (sort-by :t1))))

(defn find-first-intersection-of-ray [origin anchor environment]
  (let [direction-vector (sub anchor origin)
        line-end (-> direction-vector
                     normal
                     (mul 1000)
                     (add origin))
        intersections (find-intersections origin line-end environment)
        origin-inside? (inside-environment? origin environment)
        select-fn (if origin-inside?
                    first
                    second)]
    (-> intersections
        (->> (filter (comp pos? :t1)))
        select-fn
        (select-keys [:x :y :t1 :t2]))))

(defn find-first-intersection-of-ray-dumb [origin direction-vector environment]
  (let [line-end (-> direction-vector
                     normal
                     (mul 1000)
                     (add origin))
        intersections (find-intersections origin line-end environment)]
    (-> intersections
        (->> (filter (comp pos? :t1)))
        first
        (select-keys [:x :y :t1 :t2]))))

(defn angle-to-point [p1 p2]
  (let [d (sub p2 p1)
        angle-rad (Math/atan2 (:y d) (:x d))]
    (math/to-deg angle-rad)))

(defn angle-between-vectors [v1 v2]
  (let [a1 (angle-to-point (v 0 0) v1)
        a2 (angle-to-point (v 0 0) v2)
        angle (-> (- a1 a2)
                  math/normalize-angle)]
    (if (> angle 180)
      (- angle 180)
      angle)))

(defn environment-intersections [from to environment]
  (let [direction (normal (sub to from))
        intersections (find-intersections
                       (sub from (mul direction 1000))
                       (add to (mul direction 1000))
                       environment)]
    [(first intersections) (last intersections)]))
