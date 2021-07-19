(ns heraldry.coat-of-arms.field.type.per-bend
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(def field-type
  :heraldry.field.type/per-bend)

(defmethod interface/display-name field-type [_] "Per bend")

(defmethod interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/render-field field-type
  [path environment context]
  (let [line (options/sanitized-value (conj path :line) context)
        origin (options/sanitized-value (conj path :origin) context)
        anchor (options/sanitized-value (conj path :anchor) context)
        outline? (or (options/render-option :outline? context)
                     (options/sanitized-value (conj path :outline?) context))
        points (:points environment)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        direction (v/- anchor-point origin-point)
        direction (v/normal (v/v (-> direction :x Math/abs)
                                 (-> direction :y Math/abs)))
        initial-diagonal-start (-> direction
                                   (v/* -1000)
                                   (v/+ origin-point))
        initial-diagonal-end (-> direction
                                 (v/* 1000)
                                 (v/+ origin-point))
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
                      (v/* required-extra-length))
        diagonal-start (v/- real-diagonal-start extra-dir)
        diagonal-end (v/+ real-diagonal-end extra-dir)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         diagonal-start diagonal-end
                                         :context context
                                         :environment environment)
        parts [[["M" (v/+ diagonal-start
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/+ diagonal-end
                                      line-one-end)
                                 (v/+ diagonal-start
                                      line-one-start)])
                 "z"]
                [real-diagonal-start
                 top-right
                 real-diagonal-end]]

               [["M" (v/+ diagonal-start
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :top]
                                [(v/+ diagonal-end
                                      line-one-end)
                                 (v/+ diagonal-start
                                      line-one-start)])
                 "z"]
                [real-diagonal-start
                 real-diagonal-end
                 bottom-left]]]]
    [:<>
     [shared/make-subfields2
      path parts
      [:all nil]
      environment context]
     (line/render line [line-one-data] diagonal-start outline? context)]))
