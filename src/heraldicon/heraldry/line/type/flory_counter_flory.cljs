(ns heraldicon.heraldry.line.type.flory-counter-flory
  (:require [heraldicon.math.vector :as v]))

(defn- mirror [v]
  (v/dot v (v/Vector. -1 1)))

(defn- flip [v]
  (v/dot v (v/Vector. 1 -1)))

(def pattern
  {:display-name :string.line.type/flory-counter-flory
   :function (fn [{:keys [eccentricity
                          height
                          width
                          spacing]}
                  _line-options]
               (let [dx (* (-> eccentricity
                               (* 2)))
                     dx2 (* (-> eccentricity
                                (* 0.5)
                                (+ 0.75)))
                     dx2 (Math/sqrt dx2)
                     dx3 1
                     half-width (/ width 4)
                     base-width (* (* 0.6 dx2) half-width)
                     half-base-width (/ base-width 2)
                     ds (* half-base-width dx)
                     base-dist (/ base-width 2)
                     base-left (v/Vector. (- half-width base-dist) 0)
                     base-right (v/Vector. (+ half-width base-dist) 0)
                     point-dist (* 1.1 dx3 base-width)
                     point-y (- (* 0.9 dx3 (/ base-width 2)))
                     point-left (v/Vector. (- half-width point-dist) point-y)
                     point-right (v/Vector. (+ half-width point-dist) point-y)
                     tip (v/Vector. half-width (- (* 3 0.5 half-width)))
                     top-dist (/ base-width 5)
                     top-y (- (* 1 dx3 0.5 half-width))
                     top-left (v/Vector. (- half-width top-dist) top-y)
                     top-right (v/Vector. (+ half-width top-dist) top-y)
                     anchor1 (v/Vector. ds (- ds))
                     anchor2 (v/Vector. 0 (* -1.5 ds dx2))
                     anchor3 (v/Vector. (* -1.75 ds dx2) (* -1.5 ds dx2))
                     anchor4 (v/Vector. (* -1 ds dx2) (* -2 ds dx2))
                     anchor5 (v/Vector. 0 (* -1.5 ds dx2))
                     anchor6 (v/Vector. (* -1.75 ds dx2) (* 1.75 ds dx2))

                     tip (v/dot tip (v/Vector. 1 height))
                     top-left (v/dot top-left (v/Vector. 1 height))
                     top-right (v/dot top-right (v/Vector. 1 height))
                     point-left (v/dot point-left (v/Vector. 1 height))
                     point-right (v/dot point-right (v/Vector. 1 height))
                     anchor1 (v/dot anchor1 (v/Vector. 1 height))
                     anchor2 (v/dot anchor2 (v/Vector. 1 height))
                     anchor3 (v/dot anchor3 (v/Vector. 1 height))
                     anchor4 (v/dot anchor4 (v/Vector. 1 height))
                     anchor5 (v/dot anchor5 (v/Vector. 1 height))
                     anchor6 (v/dot anchor6 (v/Vector. 1 height))

                     flory ["l"
                            base-left

                            "c"
                            anchor1
                            (v/add (v/sub point-left base-left) anchor2)
                            (v/sub point-left base-left)

                            "c"
                            anchor3
                            (v/add (v/sub top-left point-left) anchor4)
                            (v/sub top-left point-left)

                            "c"
                            anchor5
                            (v/add (v/sub tip top-left) anchor6)
                            (v/sub tip top-left)

                            "c"
                            (mirror anchor6)
                            (v/add (v/sub top-right tip) (mirror anchor5))
                            (v/sub top-right tip)

                            "c"
                            (mirror anchor4)
                            (v/add (v/sub point-right top-right) (mirror anchor3))
                            (v/sub point-right top-right)

                            "c"
                            (mirror anchor2)
                            (v/add (v/sub base-right point-right) (mirror anchor1))
                            (v/sub base-right point-right)

                            "l" (- (* 2 half-width) (:x base-right)) 0]

                     counter-flory (mapv (fn [v]
                                           (if (instance? v/Vector v)
                                             (flip v)
                                             v))
                                         flory)]

                 {:pattern (vec (concat flory
                                        ["l" (* width spacing) 0]
                                        counter-flory))

                  :min (:y tip)
                  :max 0}))})
