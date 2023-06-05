(ns heraldicon.heraldry.line.type.embattled-cupola
  (:require
   [heraldicon.util.core :as util]))

(def pattern
  {:display-name :string.line.type/embattled-cupola
   :function (fn [{:keys [height
                          width
                          eccentricity]}
                  _line-options]
               (let [half-width (/ width 2)
                     quarter-width (/ width 4)
                     height (* half-width height)
                     eccentricity-factor (util/interpolate [[0 1]
                                                            [0.5 0.5]
                                                            [1 (/ 1 3)]] eccentricity)
                     dome-width (* half-width eccentricity-factor)
                     dome-radius-x (/ dome-width 2)
                     dome-radius-y (/ dome-width 2.5)
                     dome-edge-width (/ (- half-width dome-width) 2)]
                 {:pattern ["l"
                            quarter-width 0
                            0 (- height)
                            dome-edge-width 0
                            "a" dome-radius-x dome-radius-y 0 0 1 dome-width 0
                            "l"
                            dome-edge-width 0
                            0 height
                            quarter-width 0]
                  :min (- (+ height dome-radius-y))
                  :max 0}))})
