(ns heraldry.coat-of-arms.line.type.angled
  (:require [heraldry.gettext :refer [string]]))

(def pattern
  {:display-name (string "Angled")
   :full? true
   :function (fn [{:keys [width eccentricity] :as _line-data}
                  length
                  {:keys [real-start real-end] :as _line-options}]
               (let [real-start (or real-start 0)
                     real-end (or real-end length)
                     relevant-length (- real-end real-start)
                     pos-x (-> relevant-length
                               (* eccentricity))
                     height width]
                 {:pattern ["h" real-start
                            "h" pos-x
                            "v" height
                            "h" (- relevant-length pos-x)
                            "h" (- length real-end)
                            "v" (- height)]
                  :min 0
                  :max height}))})
