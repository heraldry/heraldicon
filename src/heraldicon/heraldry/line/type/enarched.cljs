(ns heraldicon.heraldry.line.type.enarched
  (:require
   [heraldicon.util :as util]))

(def pattern
  {:display-name :string.line.type/enarched
   :full? true
   :function (fn [{:keys [height width eccentricity] :as _line-data}
                  length
                  {:keys [real-start real-end] :as _line-options}]
               (let [real-start (or real-start 0)
                     real-end (or real-end length)
                     relevant-length (- real-end real-start)
                     height (* relevant-length
                               (util/map-to-interval height 0 0.4))
                     pos-x (-> relevant-length
                               (* eccentricity))
                     orientation-1 (- 1 (/ width 100))
                     orientation-2 (- 1 orientation-1)]
                 {:pattern ["h" real-start
                            "c"
                            (-> pos-x
                                (* orientation-1)) "," (- height) " "
                            (-> relevant-length
                                (- pos-x)
                                (* orientation-2)
                                (+ pos-x)) "," (- height) " "
                            relevant-length "," 0
                            "h" (- length real-end)]
                  :min (* 0.75 (- height)) ; should be the max point of the curve at t = 0.5
                  :max 0}))})
