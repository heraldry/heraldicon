(ns heraldicon.heraldry.line.type.fir-tree-topped
  (:require
   [heraldicon.options :as options]))

(def pattern
  {:display-name :string.line.type/fir-tree-topped
   :function (fn [{:keys [eccentricity height width]}
                  _line-options]
               (let [dx (/ width 16)
                     dy (* dx height)
                     dx3 (* 3 dx)
                     extra (* dx (options/map-to-interval eccentricity 0.5 2.5))
                     extra-y (* dx (options/map-to-interval eccentricity 0.1 0.75))]
                 {:pattern ["l"
                            (+ dx extra) (-> dy (* -3) (- extra-y))
                            (* -2 extra) (* 2 extra-y)
                            (+ dx3 extra) (-> dy (* -6) (- extra-y))
                            (+ dx3 extra) (-> dy (* 6) (+ extra-y))
                            (* -2 extra) (* -2 extra-y)
                            (+ dx dx extra extra) (-> dy (* 6) (+ extra-y) (+ extra-y))
                            (* -2 extra) (* -2 extra-y)
                            (+ dx3 extra) (-> dy (* 6) (+ extra-y))
                            (+ dx3 extra) (-> dy (* -6) (- extra-y))
                            (* -2 extra) (* 2 extra-y)
                            (+ dx extra) (-> dy (* -3) (- extra-y))]
                  :min (+ (-> dy (* -3) (- extra-y))
                          (* 2 extra-y)
                          (-> dy (* -6) (- extra-y)))
                  :max (- (+ (-> dy (* -3) (- extra-y))
                             (* 2 extra-y)
                             (-> dy (* -6) (- extra-y))))}))})
