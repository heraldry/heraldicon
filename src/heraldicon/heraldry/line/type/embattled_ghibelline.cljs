(ns heraldicon.heraldry.line.type.embattled-ghibelline
  (:require
   [heraldicon.util.core :as util]))

(def pattern
  {:display-name :string.line.type/embattled-ghibelline
   :function (fn [{:keys [height
                          width
                          eccentricity]}
                  _line-options]
               (let [half-width (/ width 2)
                     quarter-width (/ width 4)
                     height (* half-width height)
                     eccentricity-factor (util/interpolate [[0 (/ 1 3)]
                                                            [0.5 1]
                                                            [1 2]] eccentricity)
                     dome-width half-width
                     dome-radius-x (/ dome-width 2)
                     dome-radius-y (* dome-radius-x eccentricity-factor)]
                 {:pattern ["l"
                            quarter-width 0
                            0 (- height)
                            0 (- dome-radius-y)
                            "a" dome-radius-x dome-radius-y 0 0 1 dome-radius-x dome-radius-y
                            "a" dome-radius-x dome-radius-y 0 0 1 dome-radius-x (- dome-radius-y)
                            "l"
                            0 dome-radius-y
                            0 height
                            quarter-width 0]
                  :min (- (+ height dome-radius-y))
                  :max 0}))})
