(ns heraldry.coat-of-arms.field.type.tierced-per-pairle
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]))

(def field-type :heraldry.field.type/tierced-per-pairle)

(defmethod field-interface/display-name field-type [_] "Tierced per pairle")

(defmethod field-interface/part-names field-type [_] ["middle" "side I" "side II"])

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        opposite-line (interface/get-sanitized-data (conj path :opposite-line) context)
        extra-line (interface/get-sanitized-data (conj path :extra-line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        anchor (interface/get-sanitized-data (conj path :anchor) context)
        direction-anchor (interface/get-sanitized-data (conj path :direction-anchor) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        raw-direction-anchor (interface/get-raw-data (conj path :direction-anchor) context)
        direction-anchor (cond-> direction-anchor
                           (-> direction-anchor
                               :point
                               #{:left
                                 :right
                                 :top
                                 :bottom}) (->
                                            (assoc :offset-x (or (:offset-x raw-direction-anchor)
                                                                 (:offset-x origin)))
                                            (assoc :offset-y (or (:offset-y raw-direction-anchor)
                                                                 (:offset-y origin)))))
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        {direction-origin-point :real-origin
         direction-anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                               environment
                                               origin
                                               direction-anchor
                                               0
                                               -90)
        pall-angle (v/normalize-angle
                    (v/angle-to-point direction-origin-point
                                      direction-anchor-point))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     pall-angle)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point pall-angle unadjusted-origin-point origin-point)
                                           (chevron/mirror-point pall-angle unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-right relative-left] (chevron/arm-diagonals pall-angle origin-point anchor-point)
        diagonal-left (v/+ origin-point relative-left)
        diagonal-right (v/+ origin-point relative-right)
        straight (v/+ origin-point (v/* (v/+ relative-left relative-right) -1))
        intersection-left (v/find-first-intersection-of-ray origin-point diagonal-left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point diagonal-right environment)
        intersection-straight (v/find-first-intersection-of-ray origin-point straight environment)
        end-left (-> intersection-left
                     (v/- origin-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/- origin-point)
                      v/abs)
        end (max end-left end-right)
        {line-left :line
         line-left-start :line-start} (line/create line
                                                   origin-point diagonal-left
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end end
                                                   :context context
                                                   :environment environment)
        {line-right :line
         line-right-end :line-end} (line/create opposite-line
                                                origin-point diagonal-right
                                                :flipped? true
                                                :mirrored? true
                                                :real-start 0
                                                :real-end end
                                                :context context
                                                :environment environment)
        {line-straight :line
         line-straight-start :line-start} (line/create extra-line
                                                       origin-point intersection-straight
                                                       :flipped? true
                                                       :mirrored? true
                                                       :context context
                                                       :environment environment)
        {line-straight-reversed :line
         line-straight-reversed-start :line-start} (line/create extra-line
                                                                origin-point intersection-straight
                                                                :reversed? true
                                                                :context context
                                                                :environment environment)
        fork-infinity-points (cond
                               (<= 45 pall-angle 135) [:left :right]
                               (<= 135 pall-angle 225) [:left :left]
                               (<= 225 pall-angle 315) [:top :top]
                               :else [:right :right])
        side-infinity-points (cond
                               (<= 45 pall-angle 135) [:bottom :top]
                               (<= 135 pall-angle 225) [:left :right]
                               (<= 225 pall-angle 315) [:top :bottom]
                               :else [:right :left])
        parts [[["M" (v/+ diagonal-left
                          line-left-start)
                 (svg/stitch line-left)
                 "L" origin-point
                 (svg/stitch line-right)
                 (infinity/path :counter-clockwise
                                fork-infinity-points
                                [(v/+ diagonal-right
                                      line-right-end)
                                 (v/+ diagonal-left
                                      line-left-start)])
                 "z"]
                [top-left
                 bottom-right]]

               [["M" (v/+ intersection-straight
                          line-straight-reversed-start)
                 (svg/stitch line-straight-reversed)
                 "L" origin-point
                 (svg/stitch line-right)
                 (infinity/path :clockwise
                                side-infinity-points
                                [(v/+ diagonal-right
                                      line-right-end)
                                 (v/+ straight
                                      line-straight-reversed-start)])
                 "z"]
                [top-left
                 bottom-right]]

               [["M" (v/+ diagonal-left
                          line-left-start)
                 (svg/stitch line-left)
                 "L" origin-point
                 (svg/stitch line-straight)
                 (infinity/path :clockwise
                                (reverse side-infinity-points)
                                [(v/+ straight
                                      line-straight-start)
                                 (v/+ diagonal-left
                                      line-left-start)])
                 "z"]
                [top-left
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      path parts
      [:all
       [(svg/make-path
         ["M" (v/+ straight
                   line-straight-reversed-start)
          (svg/stitch line-straight-reversed)])]
       nil]
      environment context]
     (when outline?
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-left
                              line-left-start)
                     (svg/stitch line-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-straight)])}]])]))
