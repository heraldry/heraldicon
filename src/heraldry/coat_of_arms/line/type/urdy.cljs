(ns heraldry.coat-of-arms.line.type.urdy
  (:require [heraldry.gettext :refer [string]]))

(def pattern
  {:display-name (string "Urdy")
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [quarter-width (/ width 4)
                     pointy-height (* quarter-width
                                      (* 2)
                                      (* (-> eccentricity
                                             (* 0.6)
                                             (+ 0.2)))
                                      (* height))
                     middle-height (* quarter-width height)
                     half-height (/ middle-height 2)]
                 {:pattern ["l"
                            [0 (- half-height)]
                            [quarter-width (- pointy-height)]
                            [quarter-width pointy-height]
                            [0 middle-height]
                            [quarter-width pointy-height]
                            [quarter-width (- pointy-height)]
                            [0 (- half-height)]]
                  :min (+ (- half-height)
                          (- pointy-height))
                  :max (+ half-height
                          pointy-height)}))})
