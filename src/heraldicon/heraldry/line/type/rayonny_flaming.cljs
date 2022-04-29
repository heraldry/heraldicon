(ns heraldicon.heraldry.line.type.rayonny-flaming
  (:require
   [heraldicon.math.vector :as v]
   [heraldicon.util :as util]))

(defn curvy-line [direction eccentricity flipped?]
  (let [length (-> direction
                   v/abs
                   (/ 2))
        middle-height (* length (util/map-to-interval eccentricity -0.1 0.2))
        orientation1-height (- (* length (util/map-to-interval eccentricity 0.1 0.9)))
        angle (v/angle-to-point (v/v 0 0) direction)
        orientation1 (v/v (* length 0.5) orientation1-height)
        orientation2 (v/v (* length 0.75) (- middle-height))
        middle (v/v length (- middle-height))
        orientation3 (v/v (- length (* length 0.75)) 0)
        orientation4 (v/v (- length (* length 0.5)) (+ middle-height
                                                       orientation1-height))
        end (v/v length middle-height)
        vf (if flipped?
             (v/v 1 -1)
             (v/v 1 1))]
    ["c"
     (v/rotate (v/dot orientation1 vf) angle)
     (v/rotate (v/dot orientation2 vf) angle)
     (v/rotate (v/dot middle vf) angle)
     "c"
     (v/rotate (v/dot orientation3 vf) angle)
     (v/rotate (v/dot orientation4 vf) angle)
     (v/rotate (v/dot end vf) angle)]))

(def pattern
  {:display-name :string.line.type/rayonny-flaming
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [quarter-width (/ width 4)
                     height (* 1.2 width height)
                     line-up (curvy-line (v/v quarter-width (- height)) eccentricity true)
                     line-down (curvy-line (v/v quarter-width height) eccentricity false)]
                 {:pattern (concat line-up line-down line-up line-down)
                  :min (- height)
                  :max 0}))})
