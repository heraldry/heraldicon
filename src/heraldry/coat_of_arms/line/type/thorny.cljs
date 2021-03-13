(ns heraldry.coat-of-arms.line.type.thorny
  (:require [heraldry.util :as util]))

(defn pattern
  {:display-name "Thorny"
   :value        :thorny}
  [{:keys [eccentricity
           height
           width]}
   _line-options]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        height        (* half-width height)
        rf            (util/map-to-interval eccentricity 1.5 1)
        r             (* quarter-width rf rf)]
    ["l" [quarter-width 0]
     "a" r r 0 0 0 [0 (- height)]
     "l" [half-width 0]
     "a" r r 0 0 1 [0 height]
     "l" [quarter-width 0]]))
