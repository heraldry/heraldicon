(ns heraldicon.heraldry.line.type.rayonny-flaming
  (:require
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]))

(defn curvy-line [direction eccentricity flipped?]
  (let [length (-> direction
                   v/abs
                   (/ 2))
        middle-height (* length (options/map-to-interval eccentricity -0.1 0.2))
        orientation1-height (- (* length (options/map-to-interval eccentricity 0.1 0.9)))
        angle (v/angle-to-point v/zero direction)
        orientation1 (v/Vector. (* length 0.5) orientation1-height)
        orientation2 (v/Vector. (* length 0.75) (- middle-height))
        middle (v/Vector. length (- middle-height))
        orientation3 (v/Vector. (- length (* length 0.75)) 0)
        orientation4 (v/Vector. (- length (* length 0.5)) (+ middle-height
                                                             orientation1-height))
        end (v/Vector. length middle-height)
        vf (if flipped?
             (v/Vector. 1 -1)
             (v/Vector. 1 1))]
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
                     line-up (curvy-line (v/Vector. quarter-width (- height)) eccentricity true)
                     line-down (curvy-line (v/Vector. quarter-width height) eccentricity false)]
                 {:pattern (concat line-up line-down line-up line-down)
                  :min (- height)
                  :max 0}))})
