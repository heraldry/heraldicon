(ns heraldry.coat-of-arms.line.type.embattled-in-crosses
  (:require [heraldry.gettext :refer [string]]
            [heraldry.util :as util]))

(def pattern
  {:display-name (string "Embattled in crosses")
   :function (fn [{:keys [height
                          eccentricity
                          width]}
                  _line-options]
               (let [l (-> width (/ 4) (* (util/map-to-interval eccentricity 0.6 1.4)))
                     t (-> width (/ 2) (- l))]
                 {:pattern ["l"
                            [(/ t 2) 0]
                            [0 (- (* l height))]
                            [l 0]
                            [0 (- (* t height))]
                            [(- l) 0]
                            [0 (- (* t height))]
                            [l 0]
                            [0 (- (* l height))]
                            [t 0]
                            [0 (* l height)]
                            [l 0]
                            [0 (* t height)]
                            [(- l) 0]
                            [0 (* t height)]
                            [l 0]
                            [0 (* l height)]
                            [(/ t 2) 0]]
                  :min (- 0
                          (* l height)
                          (* t height)
                          (* t height)
                          (* l height))
                  :max 0}))})
