(ns heraldry.coat-of-arms.line.type.indented
  (:require [heraldry.gettext :refer [string]]))

(defn pattern
  {:display-name (string "Indented")
   :value :indented}
  [{:keys [height
           width]}
   _line-options]
  (let [half-width (/ width 2)
        height (* half-width height)]
    {:pattern ["l"
               [half-width (- height)]
               [half-width height]]
     :min (- height)
     :max 0}))
