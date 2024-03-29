(ns heraldicon.math.vector
  (:refer-clojure :exclude [abs])
  (:require
   ["paper" :refer [Path]]
   [heraldicon.math.angle :as angle]))

(defrecord ^:export Vector [^js/Number x ^js/Number y])

(def zero
  (Vector. 0 0))

(defn add
  (^Vector [] zero)
  (^Vector [^Vector v] v)
  (^Vector [^Vector {x1 :x y1 :y} ^Vector {x2 :x y2 :y}]
   (Vector. (+ x1 x2) (+ y1 y2))))

(defn sub
  (^Vector [] zero)
  (^Vector [^Vector {x :x y :y}] (Vector. (- x) (- y)))
  (^Vector [^Vector {x1 :x y1 :y} ^Vector {x2 :x y2 :y}]
   (Vector. (- x1 x2) (- y1 y2))))

(defn mul
  (^Vector [^Vector v] v)
  (^Vector [^Vector {x :x y :y} ^js/Number f]
   (Vector. (* x f) (* y f))))

(defn div
  (^Vector [^Vector v] v)
  (^Vector [^Vector {x :x y :y} ^js/Number f]
   (Vector. (/ x f) (/ y f))))

(defn dot ^Vector [^Vector {x1 :x y1 :y} ^Vector {x2 :x y2 :y}]
  (Vector. (* x1 x2) (* y1 y2)))

(defn cross ^js/Number [^Vector {x1 :x y1 :y} ^Vector {x2 :x y2 :y}]
  (- (* x1 y2) (* x2 y1)))

(defn abs ^js/Number [^Vector {:keys [x y]}]
  (Math/sqrt (+ (* x x) (* y y))))

(defn dot-product ^js/Number [^Vector v1 ^Vector v2]
  (let [v1-length (abs v1)
        v2-length (abs v2)
        {:keys [x y]} (dot v1 v2)]
    (if (or (zero? v1-length)
            (zero? v2-length))
      0
      (-> (+ x y)
          (/ v1-length)
          (/ v2-length)))))

(defn normal ^Vector [^Vector v]
  (let [d (abs v)]
    (if (pos? d)
      (div v d)
      v)))

(defn avg
  (^Vector [^Vector v1 ^Vector v2] (div (add v1 v2) 2))
  (^Vector [^Vector v1 ^Vector v2 & more] (div (reduce add (add v1 v2) more) (+ (count more) 2))))

(defn rotate ^Vector [{:keys [x y]} ^js/Number angle]
  (let [rad (angle/to-rad angle)]
    (Vector. (- (* x (Math/cos rad))
                (* y (Math/sin rad)))
             (+ (* x (Math/sin rad))
                (* y (Math/cos rad))))))

(defn ^:export distance-point-to-line ^js/Number [^Vector {x0 :x y0 :y}
                                                  ^Vector {x1 :x y1 :y :as v1}
                                                  ^Vector {x2 :x y2 :y :as v2}]
  (/ (Math/abs (- (* (- x2 x1)
                     (- y1 y0))
                  (* (- x1 x0)
                     (- y2 y1))))
     (abs (sub v1 v2))))

(defn line-intersection ^Vector [^Vector {x1 :x y1 :y}
                                 ^Vector {x2 :x y2 :y :as end1}
                                 ^Vector {x3 :x y3 :y :as start2}
                                 ^Vector {x4 :x y4 :y}]
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
      (div (Vector. (- (* (- (* x1 y2)
                             (* y1 x2))
                          (- x3 x4))
                       (* (- x1 x2)
                          (- (* x3 y4)
                             (* y3 x4))))
                    (- (* (- (* x1 y2)
                             (* y1 x2))
                          (- y3 y4))
                       (* (- y1 y2)
                          (- (* x3 y4)
                             (* y3 x4)))))
           D))))

(defn ^:export tangent-point ^Vector [^Vector {cx :x cy :y}
                                      ^js/Number r
                                      ^Vector {px :x py :y}]
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
        (Vector. (-> (+ (* r2 dx)
                        (* dir-factor r dy sqrtD))
                     (/ sum-d2)
                     (+ cx))
                 (-> (- (* r2 dy)
                        (* dir-factor r dx sqrtD))
                     (/ sum-d2)
                     (+ cy)))))))

(defn outer-tangent-between-circles [^Vector {x0 :x y0 :y}
                                     ^js/Number r0
                                     ^Vector {x1 :x y1 :y}
                                     ^js/Number r1
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
    [(Vector. (+ x0 (* dir-factor r0 (Math/sin alpha)))
              (+ y0 (* dir-factor r0 (Math/cos alpha))))
     (Vector. (+ x1 (* dir-factor r1 (Math/sin alpha)))
              (+ y1 (* dir-factor r1 (Math/cos alpha))))]))

(defn inner-tangent-between-circles [^Vector {x0 :x y0 :y}
                                     ^js/Number r0
                                     ^Vector {x1 :x y1 :y}
                                     ^js/Number r1
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
    [(Vector. (- x0 (* dir-factor r0 (Math/sin alpha)))
              (- y0 (* dir-factor r0 (Math/cos alpha))))
     (Vector. (+ x1 (* dir-factor r1 (Math/sin alpha)))
              (+ y1 (* dir-factor r1 (Math/cos alpha))))]))

(defn orthogonal ^Vector [^Vector {:keys [x y]}]
  (Vector. y (- x)))

(defn ->str ^js/String [^Vector {:keys [x y]}]
  (str x "," y))

(def ^:private path-intersection
  (memoize
   (fn path-intersection [path1 path2]
     (try
       (let [p1 (new Path path1)
             p2 (new Path path2)]
         (into []
               (map (fn [^js/Object location]
                      (assoc (Vector. (.. location -point -x)
                                      (.. location -point -y))
                             :t1 (/ (.. location -offset) (.-length p1))
                             :t2 (/ (.. location -intersection -offset) (.-length p2)))))
               (.getIntersections p1 p2)))
       (catch :default _
         ; TODO: This is a hack, sometimes moving ordinaries around will result in
         ; a temporary situation where the moved ordinary is still rendered in the wrong
         ; place, with certain properties becoming nil or otherwise unreliable.
         ; In that case these paths might contain "Infinity", which would crash the program,
         ; but a frame afterwards everything is updated properly and all is well.
         ; So for now catch this error and assume there's no intersection.
         [])))))

(defn angle-to-point ^js/Number [^Vector v1 ^Vector v2]
  (let [d (sub v2 v1)
        angle-rad (Math/atan2 (:y d) (:x d))]
    (angle/to-deg angle-rad)))

(defn arc-angle-between-vectors ^js/Number [^Vector v1 ^Vector v2]
  (let [a1 (angle-to-point zero v1)
        a2 (angle-to-point zero v2)]
    (angle/normalize (- a2 a1))))

(defn angle-between-vectors ^js/Number [^Vector v1 ^Vector v2]
  (let [a1 (angle-to-point zero v1)
        a2 (angle-to-point zero v2)
        angle (angle/normalize (- a1 a2))]
    (if (> angle 180)
      (- angle 180)
      angle)))

(defn intersections-with-shape [from to shape & {:keys [default?]}]
  (let [direction (normal (sub to from))
        inf (mul direction 1000)
        line-path (str "M" (->str (sub from inf))
                       "L" (->str (add to inf)))
        intersections (sort-by :t1 (path-intersection line-path shape))
        first-intersection (first intersections)
        last-intersection (last intersections)]
    (if (and default?
             (not last-intersection))
      [from to]
      [first-intersection last-intersection])))

(defn last-intersection-with-shape [from to shape & {:keys [default?
                                                            relative?]}]
  (let [direction (if relative?
                    to
                    (sub to from))
        direction (normal direction)
        inf (mul direction 1000)
        line-path (str "M" (->str from)
                       "L" (->str (add from inf)))
        intersections (sort-by :t1 (filter (comp pos? :t1) (path-intersection line-path shape)))
        last-intersection (last intersections)]
    (if (and default?
             (not last-intersection))
      (if relative?
        (add to from)
        to)
      last-intersection)))
