(ns heraldry.coat-of-arms.field.type.per-bend-sinister
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.vector.core :as v]
            [heraldry.interface :as interface]
            [heraldry.vector.svg :as svg]))

(def field-type :heraldry.field.type/per-bend-sinister)

(defmethod field-interface/display-name field-type [_] "Per bend sinister")

(defmethod field-interface/part-names field-type [_] ["chief" "base"])

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        anchor (interface/get-sanitized-data (conj path :anchor) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))

        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        direction (v/sub anchor-point origin-point)
        direction (v/normal (v/v (-> direction :x Math/abs)
                                 (-> direction :y Math/abs -)))
        initial-diagonal-start (-> direction
                                   (v/mul -1000)
                                   (v/add origin-point))
        initial-diagonal-end (-> direction
                                 (v/mul 1000)
                                 (v/add origin-point))
        [real-diagonal-start
         real-diagonal-end] (v/environment-intersections
                             initial-diagonal-start
                             initial-diagonal-end
                             environment)
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        extra-dir (-> direction
                      (v/mul required-extra-length))
        diagonal-start (v/sub real-diagonal-start extra-dir)
        diagonal-end (v/add real-diagonal-end extra-dir)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         diagonal-start diagonal-end
                                         :context context
                                         :environment environment)
        parts [[["M" (v/add diagonal-start
                            line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:top :left]
                                [(v/add diagonal-end
                                        line-one-end)
                                 (v/add diagonal-start
                                        line-one-start)])
                 "z"]
                [real-diagonal-start
                 top-left
                 real-diagonal-end]]

               [["M" (v/add diagonal-start
                            line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:top :left]
                                [(v/add diagonal-end
                                        line-one-end)
                                 (v/add diagonal-start
                                        line-one-start)])
                 "z"]
                [real-diagonal-start
                 bottom-right
                 real-diagonal-end]]]]
    [:<>
     [shared/make-subfields
      path parts
      [:all nil]
      environment context]
     [line/render line [line-one-data] diagonal-start outline? context]]))
