(ns heraldry.coat-of-arms.field.type.per-pale
  (:require [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.vector.core :as v]
            [heraldry.interface :as interface]
            [heraldry.vector.svg :as svg]))

(def field-type :heraldry.field.type/per-pale)

(defmethod field-interface/display-name field-type [_] "Per pale")

(defmethod field-interface/part-names field-type [_] ["dexter" "sinister"])

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        real-top (assoc (:top points) :x (:x origin-point))
        real-bottom (assoc (:bottom points) :x (:x origin-point))
        bottom-right (:bottom-right points)
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        top (v/- real-top (v/v 0 required-extra-length))
        bottom (v/+ real-bottom (v/v 0 required-extra-length))
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         top
                                         bottom
                                         :context context
                                         :environment environment)

        parts [[["M" (v/+ top
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:bottom :top]
                                [(v/+ bottom
                                      line-one-end)
                                 (v/+ top
                                      line-one-start)])
                 "z"]
                [top-left
                 real-bottom]]

               [["M" (v/+ top
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:bottom :top]
                                [(v/+ bottom
                                      line-one-end)
                                 (v/+ top
                                      line-one-start)])
                 "z"]
                [real-top
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      path parts
      [:all nil]
      environment context]
     [line/render line [line-one-data] top outline? context]]))
