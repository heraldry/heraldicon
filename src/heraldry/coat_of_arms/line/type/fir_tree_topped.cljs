(ns heraldry.coat-of-arms.line.type.fir-tree-topped
  (:require
   [heraldry.gettext :refer [string]]
   [heraldry.util :as util]))

(def pattern
  {:display-name (string "Fir-tree topped")
   :function (fn [{:keys [eccentricity height width]}
                  _line-options]
               (let [dx (/ width 16)
                     dy (* dx height)
                     dx3 (* 3 dx)
                     extra (* dx (util/map-to-interval eccentricity 0.5 2.5))
                     extra-y (* dx (util/map-to-interval eccentricity 0.1 0.75))]
                 {:pattern ["l"
                            [(+ dx extra) (-> dy (* -3) (- extra-y))]
                            [(* -2 extra) (* 2 extra-y)]
                            [(+ dx3 extra) (-> dy (* -6) (- extra-y))]
                            [(+ dx3 extra) (-> dy (* 6) (+ extra-y))]
                            [(* -2 extra) (* -2 extra-y)]
                            [(+ dx dx extra extra) (-> dy (* 6) (+ extra-y) (+ extra-y))]
                            [(* -2 extra) (* -2 extra-y)]
                            [(+ dx3 extra) (-> dy (* 6) (+ extra-y))]
                            [(+ dx3 extra) (-> dy (* -6) (- extra-y))]
                            [(* -2 extra) (* 2 extra-y)]
                            [(+ dx extra) (-> dy (* -3) (- extra-y))]]
                  :min (+ (-> dy (* -3) (- extra-y))
                          (* 2 extra-y)
                          (-> dy (* -6) (- extra-y)))
                  :max (- (+ (-> dy (* -3) (- extra-y))
                             (* 2 extra-y)
                             (-> dy (* -6) (- extra-y))))}))})
