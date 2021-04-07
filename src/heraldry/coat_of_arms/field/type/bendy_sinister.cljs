(ns heraldry.coat-of-arms.field.type.bendy-sinister
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.field.type.barry :as barry]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Bendy sinister"
   :value :bendy-sinister
   :parts []}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line
                layout
                origin
                anchor]} (options/sanitize division (field-options/options division))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        top (:top points)
        bottom (:bottom points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     (update anchor :angle #(when %
                                                              (- %)))
                                     0
                                     nil)
        center-point (v/line-intersection origin-point anchor-point
                                          top bottom)
        direction (v/- anchor-point origin-point)
        direction (v/v (-> direction :x Math/abs)
                       (-> direction :y Math/abs -))
        direction-orthogonal (v/orthogonal direction)
        angle (v/angle-to-point (v/v 0 0) direction)
        required-half-width (v/distance-point-to-line top-right center-point (v/+ center-point direction-orthogonal))
        required-half-height (v/distance-point-to-line top-left center-point (v/+ center-point direction))
        [parts overlap outlines] (barry/barry-parts
                                  layout
                                  (v/v (- required-half-width) (- required-half-height))
                                  (v/v required-half-width required-half-height)
                                  line hints render-options environment)]
    [:g {:transform (str "translate(" (:x center-point) "," (:y center-point) ")"
                         "rotate(" angle ")")}
     [shared/make-subfields
      (shared/field-context-key type) fields parts
      overlap
      environment division context]
     outlines]))
