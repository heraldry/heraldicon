(ns heraldry.coat-of-arms.line.type.rayonny-spiked
  (:require
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

(defn curvy-line [direction eccentricity flipped?]
  (let [length (-> direction
                   v/abs
                   (/ 2))
        middle-height (* length (util/map-to-interval eccentricity 0.1 -0.2))
        anchor1-height (- (* length (util/map-to-interval eccentricity 0.2 0.8)))
        angle (v/angle-to-point (v/v 0 0) direction)
        anchor1 (v/v (* length 0.5) anchor1-height)
        anchor2 (v/v (* length 0.75) (- middle-height))
        middle (v/v length (- middle-height))
        anchor3 (v/v (- length (* length 0.75)) 0)
        anchor4 (if flipped?
                  (v/v (- length (* length 0.5)) (+ middle-height
                                                    anchor1-height))
                  (-> (v/v (- (* 1.5 anchor1-height)) 0)
                      (v/rotate (- (- 90 angle)))
                      (v/dot (v/v -1 1))
                      (v/add (v/v length middle-height))))
        end (v/v length middle-height)
        vf (if flipped?
             (v/v 1 -1)
             (v/v 1 1))]
    ["c"
     (v/rotate (v/dot anchor1 vf) angle)
     (v/rotate (v/dot anchor2 vf) angle)
     (v/rotate (v/dot middle vf) angle)
     "c"
     (v/rotate (v/dot anchor3 vf) angle)
     (v/rotate (v/dot anchor4 vf) angle)
     (v/rotate (v/dot end vf) angle)]))

(def pattern
  {:display-name :string.line.type/rayonny-spiked
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     quarter-width (/ half-width 2)
                     height (* 1.2 width height)
                     line-up (curvy-line (v/v (* half-width 0.4) (- height)) eccentricity true)
                     line-down (curvy-line (v/v (* half-width 0.6) height) eccentricity false)]
                 {:pattern (concat line-up line-down
                                   ["l" quarter-width (- height)]
                                   ["l" quarter-width height])
                  :min (- height)
                  :max 0}))})
