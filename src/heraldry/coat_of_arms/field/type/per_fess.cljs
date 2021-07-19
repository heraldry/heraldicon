(ns heraldry.coat-of-arms.field.type.per-fess
  (:require [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(def field-type
  :heraldry.field.type/per-fess)

(defmethod interface/display-name field-type [_] "Per fess")

(defmethod interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/render-field field-type
  [path environment context]
  (let [line (options/sanitized-value (conj path :line) context)
        origin (options/sanitized-value (conj path :origin) context)
        outline? (or (options/render-option :outline? context)
                     (options/sanitized-value (conj path :outline?) context))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        real-left (assoc (:left points) :y (:y origin-point))
        real-right (assoc (:right points) :y (:y origin-point))
        effective-width (or (:width line) 1)
        effective-width (cond-> effective-width
                          (:spacing line) (+ (* (:spacing line) effective-width)))
        required-extra-length (-> 30
                                  (/ effective-width)
                                  Math/ceil
                                  inc
                                  (* effective-width))
        left (v/- real-left (v/v required-extra-length 0))
        right (v/+ real-right (v/v required-extra-length 0))
        bottom-right (:bottom-right points)
        {line-one :line
         line-one-start :line-start
         line-one-end :line-end
         :as line-one-data} (line/create line
                                         left right
                                         :context context
                                         :environment environment)
        parts [[["M" (v/+ left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/+ right
                                      line-one-end)
                                 (v/+ left
                                      line-one-start)])
                 "z"]
                [top-left
                 real-right]]

               [["M" (v/+ left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :left]
                                [(v/+ right
                                      line-one-end)
                                 (v/+ left
                                      line-one-start)])
                 "z"]
                [real-left
                 bottom-right]]]]
    [:<>
     [shared/make-subfields2
      path parts
      [:all nil]
      environment context]
     (line/render line [line-one-data] left outline? context)]))
