(ns heraldry.coat-of-arms.division.type.bendy-sinister
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.division.type.barry :as barry]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Bendy sinister"
   :value :bendy-sinister
   :parts []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line
                layout
                origin
                diagonal-mode]} (options/sanitize division (division-options/options division))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        origin-point (position/calculate origin environment :fess)
        direction (angle/direction diagonal-mode points origin-point)
        direction-orthogonal (v/v (-> direction :y) (-> direction :x -))
        angle (angle/angle-to-point (v/v 0 0) (v/dot direction (v/v 1 -1)))
        required-half-width (v/distance-point-to-line top-right origin-point (v/+ origin-point direction))
        required-half-height (v/distance-point-to-line top-left origin-point (v/+ origin-point direction-orthogonal))
        [parts overlap outlines] (barry/barry-parts
                                  layout
                                  (v/v (- required-half-width) (- required-half-height))
                                  (v/v required-half-width required-half-height)
                                  line hints render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")")}
     [shared/make-division
      (shared/division-context-key type) fields parts
      overlap
      outlines
      environment division context]]))
