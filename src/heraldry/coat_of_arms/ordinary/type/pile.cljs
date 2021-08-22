(ns heraldry.coat-of-arms.ordinary.type.pile
  (:require [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.shared.pile :as pile]
            [heraldry.vector.core :as v]
            [heraldry.interface :as interface]
            [heraldry.vector.svg :as svg]
            [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/pile)

(defmethod ordinary-interface/display-name ordinary-type [_] "Pile")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        opposite-line (interface/get-sanitized-data (conj path :opposite-line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        anchor (interface/get-sanitized-data (conj path :anchor) context)
        geometry (interface/get-sanitized-data (conj path :geometry) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        width (:width environment)
        height (:height environment)
        thickness-base (if (#{:left :right} (:point origin))
                         height
                         width)
        {origin-point :origin
         point :point
         thickness :thickness} (pile/calculate-properties
                                environment
                                origin
                                (cond-> anchor
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point origin)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                thickness-base
                                (case (:point origin)
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        pile-angle (v/angle-to-point point origin-point)
        {left-point :left
         right-point :right} (pile/diagonals origin-point point thickness)
        intersection-left (-> (v/environment-intersections point left-point environment)
                              last)
        intersection-right (-> (v/environment-intersections point right-point environment)
                               last)
        joint-angle (v/angle-between-vectors (v/sub intersection-left point)
                                             (v/sub intersection-right point))
        end-left (-> intersection-left
                     (v/sub point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of thickness-base))
                 (update-in [:fimbriation :thickness-2] (util/percent-of thickness-base)))
        {line-left :line
         line-left-start :line-start
         line-left-min :line-min
         :as line-left-data} (line/create line
                                          point left-point
                                          :reversed? true
                                          :real-start 0
                                          :real-end end
                                          :context context
                                          :environment environment)
        {line-right :line
         :as line-right-data} (line/create opposite-line
                                           point right-point
                                           :real-start 0
                                           :real-end end
                                           :context context
                                           :environment environment)
        part [["M" (v/add left-point
                          line-left-start)
               (svg/stitch line-left)
               (svg/stitch line-right)
               "z"]
              [top-left top-right
               bottom-left bottom-right]]]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     [line/render line [line-left-data
                        line-right-data] left-point outline? context]
     [cottising/render-chevron-cottise
      :cottise-1 :cottise-2 :cottise-1
      path environment context
      :distance-fn (fn [distance half-joint-angle-rad]
                     (-> (- distance)
                         (/ 100)
                         (* thickness-base)
                         (+ line-left-min)
                         (/ (if (zero? half-joint-angle-rad)
                              0.00001
                              (Math/sin half-joint-angle-rad)))))
      :alignment :left
      :width width
      :height height
      :chevron-angle pile-angle
      :joint-angle joint-angle
      :corner-point point]]))
