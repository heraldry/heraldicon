(ns heraldicon.math.curve.catmullrom
  (:require
   [heraldicon.math.vector :as v]))

(defn smooth-point ^v/Vector [main-fn ^v/Vector p0 ^v/Vector p1 ^v/Vector p2 ^js/Number tension]
  (main-fn p1
           (v/mul (v/sub p2 p0)
                  (/ tension 6))))

(defn calculate-cubic-bezier-curve [^js/Number tension
                                    [^v/Vector p0
                                     ^v/Vector p1
                                     ^v/Vector p2
                                     ^v/Vector p3]]
  [p1
   (smooth-point v/add p0 p1 p2 tension)
   (smooth-point v/sub p1 p2 p3 tension)
   p2])

(defn catmullrom [points & {:keys [^js/Number tension] :or {tension 1}}]
  (->> (concat [(first points)] points [(last points)])
       (partition 4 1)
       (map (partial calculate-cubic-bezier-curve tension))))
