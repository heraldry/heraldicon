(ns heraldry.coat-of-arms.line.type.wolf-toothed
  (:require
   [heraldry.util :as util]))

(defn pattern
  {:display-name {:en "Wolf toothed"
                  :de "Wolfzahnschnitt"}
   :value :wolf-toothed}
  [{:keys [eccentricity
           height
           width]}
   _line-options]
  (let [half-width (/ width 2)
        height (* width height)
        rf (util/map-to-interval eccentricity 1.5 1.2)
        r (* (max (/ height 2)
                  half-width) rf rf)
        shift (* (util/map-to-interval eccentricity -0.5 0.5)
                 half-width)]
    {:pattern ["a" (* r 0.9) (* r 0.9) 0 0 0 [(- half-width shift) (- height)]
               "a" r r 0 0 1 [(+ half-width
                                 shift) height]]
     :min (- height)
     :max 0}))
