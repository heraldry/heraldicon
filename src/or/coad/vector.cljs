(ns or.coad.vector)

(defn v [x y]
  {:x x
   :y y})

#_{:clj-kondo/ignore [:redefined-var]}
(defn + [{x1 :x y1 :y} {x2 :x y2 :y}]
  {:x (cljs.core/+ x1 x2)
   :y (cljs.core/+ y1 y2)})

#_{:clj-kondo/ignore [:redefined-var]}
(defn - [{x1 :x y1 :y} & [{x2 :x y2 :y :as second}]]
  (if second
    {:x (cljs.core/- x1 x2)
     :y (cljs.core/- y1 y2)}
    {:x (cljs.core/- x1)
     :y (cljs.core/- y1)}))

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
