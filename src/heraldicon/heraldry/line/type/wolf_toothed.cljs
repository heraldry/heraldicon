(ns heraldicon.heraldry.line.type.wolf-toothed
  (:require
   [heraldicon.options :as options]))

(def pattern
  {:display-name :string.line.type/wolf-toothed
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     height (* width height)
                     rf (options/map-to-interval eccentricity 1.5 1.2)
                     r (* (max (/ height 2)
                               half-width) rf rf)
                     shift (* (options/map-to-interval eccentricity -0.5 0.5)
                              half-width)]
                 {:pattern ["a" (* r 0.9) (* r 0.9) 0 0 0 [(- half-width shift) (- height)]
                            "a" r r 0 0 1 [(+ half-width
                                              shift) height]]
                  :min (- height)
                  :max 0}))})
