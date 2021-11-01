(ns heraldry.coat-of-arms.field.type.bendy
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.field.type.barry :as barry]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]))

(def field-type :heraldry.field.type/bendy)

(defmethod field-interface/display-name field-type [_] {:en "Bendy"
                                                        :de "Schräggeteilt vielfach"})

(defmethod field-interface/part-names field-type [_] nil)

(defmethod field-interface/render-field field-type
  [{:keys [path environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        top (:top points)
        bottom (:bottom points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        center-point (v/line-intersection origin-point anchor-point
                                          top bottom)
        direction (v/sub anchor-point origin-point)
        direction (v/v (-> direction :x Math/abs)
                       (-> direction :y Math/abs))
        direction-orthogonal (v/orthogonal direction)
        angle (v/angle-to-point (v/v 0 0) direction)
        required-half-width (v/distance-point-to-line top-left center-point (v/add center-point direction-orthogonal))
        required-half-height (v/distance-point-to-line top-right center-point (v/add center-point direction))
        [parts overlap outlines] (barry/barry-parts
                                  (v/v (- required-half-width) (- required-half-height))
                                  (v/v required-half-width required-half-height)
                                  line outline? context)]
    [:g {:transform (str "translate(" (v/->str center-point) ")"
                         "rotate(" angle ")")}
     [shared/make-subfields
      path parts
      overlap
      environment context]
     outlines]))
