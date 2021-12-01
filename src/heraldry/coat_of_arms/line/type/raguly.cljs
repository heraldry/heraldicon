(ns heraldry.coat-of-arms.line.type.raguly
  (:require [heraldry.gettext :refer [string]]))

(defn pattern
  {:display-name (string "Raguly")
   :value :raguly}
  [{:keys [eccentricity
           height
           width]}
   _line-options]
  (let [half-width (/ width 2)
        quarter-width (/ width 4)
        height (* half-width height)
        dx (-> width
               (/ 2)
               (* (-> eccentricity
                      (* 0.7)
                      (+ 0.3))))]
    {:pattern ["l"
               [quarter-width 0]
               [(- dx) (- height)]
               [half-width 0]
               [dx height]
               [quarter-width 0]]
     :min (- height)
     :max 0}))
