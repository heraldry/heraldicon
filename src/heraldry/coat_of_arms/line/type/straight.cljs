(ns heraldry.coat-of-arms.line.type.straight
  (:require [heraldry.gettext :refer [string]]))

(defn full
  {:display-name (string "Straight")
   :value :straight
   :full? true}
  [{:keys [width]}
   _line-options]
  {:pattern ["h" width]
   :min 0
   :max 0})
