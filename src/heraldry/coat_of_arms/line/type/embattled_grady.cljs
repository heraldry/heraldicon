(ns heraldry.coat-of-arms.line.type.embattled-grady
  (:require [heraldry.gettext :refer [string]]))

(defn pattern
  {:display-name (string "Embattled grady")
   :value :embattled-grady}
  [{:keys [height
           width]}
   _line-options]
  (let [dx (/ width 4)
        dy (* dx height)]
    {:pattern ["l"
               [(/ dx 2) 0]
               [0 (- dy)]
               [dx 0]
               [0 (- dy)]
               [dx 0]
               [0 dy]
               [dx 0]
               [0 dy]
               [(/ dx 2) 0]]
     :min (+ (- dy)
             (- dy))
     :max 0}))
