(ns heraldicon.heraldry.line.type.flory-counter-flory
  (:require
   [heraldicon.heraldry.line.type.flory :as flory]
   [heraldicon.math.vector :as v]))

(defn- flip [v]
  (v/dot v (v/Vector. 1 -1)))

(def pattern
  {:display-name :string.line.type/flory-counter-flory
   :function (fn [{:keys [eccentricity
                          height
                          width
                          spacing]}
                  _line-options]
               (let [half-width (/ width 2)
                     vectors (if (< eccentricity 0.5)
                               (flory/morph flory/normal-vectors flory/low-eccentricity-vectors (- 1 (* 2 eccentricity)))
                               (flory/morph flory/normal-vectors flory/high-eccentricity-vectors (* 2 (- eccentricity 0.5))))
                     {:keys [base-left
                             base-right
                             tip
                             top-left
                             top-right
                             point-left
                             point-right
                             anchor1
                             anchor2
                             anchor3
                             anchor4
                             anchor5
                             anchor6]} (flory/scale-vectors vectors width height)

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
                            (flory/mirror anchor6)
                            (v/add (v/sub top-right tip) (flory/mirror anchor5))
                            (v/sub top-right tip)

                            "c"
                            (flory/mirror anchor4)
                            (v/add (v/sub point-right top-right) (flory/mirror anchor3))
                            (v/sub point-right top-right)

                            "c"
                            (flory/mirror anchor2)
                            (v/add (v/sub base-right point-right) (flory/mirror anchor1))
                            (v/sub base-right point-right)

                            "l" (- (* 2 half-width) (:x base-right)) 0]

                     counter-flory (mapv (fn [v]
                                           (if (instance? v/Vector v)
                                             (flip v)
                                             v))
                                         flory)]

                 {:pattern (vec (concat flory
                                        ["l" (* width (/ spacing 2)) 0]
                                        counter-flory))

                  :min (:y tip)
                  :max (:y (flip tip))
                  :remaining-spacing (* width (/ spacing 2))}))})
