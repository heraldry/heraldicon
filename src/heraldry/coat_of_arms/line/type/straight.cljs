(ns heraldry.coat-of-arms.line.type.straight
  (:require [heraldry.gettext :refer [string]]))

(def pattern
  {:display-name (string "Straight")
   :full? true
   :function (fn [{:keys [width]}
                  _line-options]
               {:pattern ["h" width]
                :min 0
                :max 0})})
