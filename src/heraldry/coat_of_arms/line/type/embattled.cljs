(ns heraldry.coat-of-arms.line.type.embattled
  (:require [heraldry.gettext :refer [string]]))

(def pattern
  {:display-name (string "Embattled")
   :function (fn [{:keys [height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     quarter-width (/ width 4)
                     height (* half-width height)]
                 {:pattern ["l"
                            [quarter-width 0]
                            [0 (- height)]
                            [half-width 0]
                            [0 height]
                            [quarter-width 0]]
                  :min (- height)
                  :max 0}))})
