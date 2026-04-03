(ns heraldicon.heraldry.line.type.trefly-counter-trefly
  (:require
   [heraldicon.heraldry.line.type.trefly :as trefly]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(defn- flip [v]
  (v/dot v (v/Vector. 1 -1)))

(def pattern
  {:display-name :string.line.type/trefly-counter-trefly
   :function (fn [{:keys [eccentricity
                          height
                          width
                          spacing]}
                  _line-options]
               (let [vectors (if (< eccentricity 0.5)
                               (trefly/morph trefly/normal-vectors trefly/low-eccentricity-vectors (- 1 (* 2 eccentricity)))
                               (trefly/morph trefly/normal-vectors trefly/high-eccentricity-vectors (* 2 (- eccentricity 0.5))))
                     {:keys [base
                             base-anchor-out
                             stem-anchor-in
                             stem
                             stem-anchor-out
                             leaf-side-0-anchor-in
                             leaf-side-0
                             leaf-side-0-anchor-out
                             leaf-side-1-anchor-in
                             leaf-side-1
                             leaf-side-1-anchor-out
                             leaf-side-2-anchor-in
                             leaf-side-2
                             leaf-side-2-anchor-out
                             leaf-middle-0-anchor-in
                             leaf-middle-0
                             leaf-middle-0-anchor-out
                             leaf-middle-1-anchor-in
                             leaf-middle-1
                             middle
                             effective-height]} (trefly/scale-vectors vectors width height)

                     trefly (flatten ["l" (* width trefly/padding) 0
                                      (path/arc base base-anchor-out stem-anchor-in stem)

                                      (path/arc stem stem-anchor-out leaf-side-0-anchor-in leaf-side-0)
                                      (path/arc leaf-side-0 leaf-side-0-anchor-out leaf-side-1-anchor-in leaf-side-1)
                                      (path/arc leaf-side-1 leaf-side-1-anchor-out leaf-side-2-anchor-in leaf-side-2)

                                      (path/arc leaf-side-2 leaf-side-2-anchor-out leaf-middle-0-anchor-in leaf-middle-0)
                                      (path/arc leaf-middle-0 leaf-middle-0-anchor-out leaf-middle-1-anchor-in leaf-middle-1)
                                      (path/arc leaf-middle-1 leaf-middle-1-anchor-in leaf-middle-0-anchor-out leaf-middle-0 :mirror-at middle)
                                      (path/arc leaf-middle-0 leaf-middle-0-anchor-in leaf-side-2-anchor-out leaf-side-2 :mirror-at middle)

                                      (path/arc leaf-side-2 leaf-side-2-anchor-in leaf-side-1-anchor-out leaf-side-1 :mirror-at middle)
                                      (path/arc leaf-side-1 leaf-side-1-anchor-in leaf-side-0-anchor-out leaf-side-0 :mirror-at middle)
                                      (path/arc leaf-side-0 leaf-side-0-anchor-in stem-anchor-out stem :mirror-at middle)

                                      (path/arc stem stem-anchor-in base-anchor-out base :mirror-at middle)

                                      "l" (* width trefly/padding) 0])
                     counter-trefly (mapv (fn [v]
                                            (if (instance? v/Vector v)
                                              (flip v)
                                              v))
                                          trefly)]
                 {:pattern (vec (concat trefly
                                        ["l" (* width (/ spacing 2)) 0]
                                        counter-trefly))

                  :min (:y effective-height)
                  :max (:y (flip effective-height))
                  :remaining-spacing (* width (/ spacing 2))}))})
