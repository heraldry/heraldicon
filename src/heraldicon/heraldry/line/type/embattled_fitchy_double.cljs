(ns heraldicon.heraldry.line.type.embattled-fitchy-double
  (:require
   [heraldicon.util.core :as util]))

(def pattern
  {:display-name :string.line.type/embattled-fitchy-double
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
                     spike-width half-width
                     spike-radius-x (/ spike-width 2)
                     spike-radius-y (* spike-radius-x eccentricity-factor)]
                 {:pattern ["l"
                            quarter-width 0
                            0 (- height)
                            0 (- spike-radius-y)
                            spike-radius-x spike-radius-y
                            spike-radius-x (- spike-radius-y)
                            0 spike-radius-y
                            0 height
                            quarter-width 0]
                  :min (- (+ height spike-radius-y))
                  :max 0}))})
