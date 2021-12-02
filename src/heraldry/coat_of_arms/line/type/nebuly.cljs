(ns heraldry.coat-of-arms.line.type.nebuly
  (:require [heraldry.gettext :refer [string]]))

(def pattern
  {:display-name (string "Nebuly")
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     dx (-> half-width
                            (* (-> eccentricity
                                   (min 1)
                                   (* 1.5))))

                     anchor-height (* half-width height)]
                 {:pattern ["c" (- dx) (- anchor-height) (+ half-width dx) (- anchor-height) half-width 0
                            "c" (- dx) anchor-height (+ half-width dx) anchor-height half-width 0]
                  :min (* 0.75 (- anchor-height)) ; should be the maximum point at t = 0.5
                  :max (* 0.75 anchor-height) ; should be the maximum point at t = 0.5
                  }))})
