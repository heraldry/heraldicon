(ns heraldry.coat-of-arms.vector)

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
  (let [diff (- to from)
        distance (abs diff)
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
