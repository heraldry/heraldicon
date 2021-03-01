(ns heraldry.coat-of-arms.angle
  (:require [heraldry.coat-of-arms.vector :as v]))

(defn angle-to-point [p1 p2]
  (let [d         (v/- p2 p1)
        angle-rad (Math/atan2 (:y d) (:x d))]
    (-> angle-rad
        (/ Math/PI)
        (* 180))))

(defn direction [diagonal-mode points & [origin]]
  (let [top-left      (:top-left points)
        top-right     (:top-right points)
        bottom-left   (:bottom-left points)
        bottom-right  (:bottom-right points)
        origin        (or origin (:fess points))
        origin-height (-> origin
                          (v/- top-left)
                          :y)
        dir           (case diagonal-mode
                        :top-left-origin     (v/- origin top-left)
                        :top-right-origin    (v/- origin top-right)
                        :bottom-left-origin  (v/- origin bottom-left)
                        :bottom-right-origin (v/- origin bottom-right)
                        (v/v origin-height origin-height))]
    (v/v (-> dir :x Math/abs)
         (-> dir :y Math/abs))))
