(ns heraldicon.heraldry.line.type.potenty
  (:require
   [heraldicon.options :as options]))

(def pattern
  {:display-name :string.line.type/potenty
   :function (fn [{:keys [height
                          eccentricity
                          width]}
                  _line-options]
               (let [l (-> width (/ 4) (* (options/map-to-interval eccentricity 0.6 1.4)))
                     t (-> width (/ 2) (- l))]
                 {:pattern ["l"
                            [(+ l (/ t 2)) 0]
                            [0 (- (* t height))]
                            [(- l) 0]
                            [0 (- (* t height))]
                            [(+ l t l) 0]
                            [0 (* t height)]
                            [(- l) 0]
                            [0 (* t height)]
                            [(+ l (/ t 2)) 0]]
                  :min (+ (- (* t height))
                          (- (* t height)))
                  :max 0}))})
