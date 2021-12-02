(ns heraldry.coat-of-arms.line.type.thorny
  (:require
   [heraldry.gettext :refer [string]]
   [heraldry.util :as util]))

(def pattern
  {:display-name (string "Thorny")
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     quarter-width (/ width 4)
                     height (* half-width height)
                     rf (util/map-to-interval eccentricity 1.5 1)
                     r (* quarter-width rf rf)]
                 {:pattern ["l" [quarter-width 0]
                            "a" r r 0 0 0 [0 (- height)]
                            "l" [half-width 0]
                            "a" r r 0 0 1 [0 height]
                            "l" [quarter-width 0]]
                  :min (- height)
                  :max 0}))})
