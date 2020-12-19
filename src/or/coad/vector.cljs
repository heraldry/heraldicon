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

(defn extend [from to l]
  (let [diff (- to from)
        distance (abs diff)
        direction (/ diff distance)]
    (-> direction
        (* l)
        (+ from))))
