(ns heraldry.math.catmullrom
  (:require [clojure.string :as string]
            [heraldry.math.bezier :as bezier]
            [heraldry.math.svg.path :as path]
            [heraldry.math.vector :as v]))

(defn smooth-point [main-fn p0 p1 p2 tension]
  (main-fn p1
           (v/mul (v/sub p2 p0)
                  (/ tension 6))))

(defn calculate-cubic-bezier-curve
  [tension [p0 p1 p2 p3]]
  (bezier/bezier p1
                 (smooth-point v/add p0 p1 p2 tension)
                 (smooth-point v/sub p1 p2 p3 tension)
                 p2))

(defn catmullrom
  [points & {:keys [tension] :or {tension 1}}]
  (->> (concat [(first points)] points [(last points)])
       (partition 4 1)
       (map (partial calculate-cubic-bezier-curve tension))))
