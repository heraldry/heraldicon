(ns heraldry.coat-of-arms.ordinary.type.pall
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/pall)

(defmethod ordinary-interface/display-name ordinary-type [_] "Pall")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        opposite-line (interface/get-sanitized-data (conj path :opposite-line) context)
        extra-line (interface/get-sanitized-data (conj path :extra-line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        anchor (interface/get-sanitized-data (conj path :anchor) context)
        direction-anchor (interface/get-sanitized-data (conj path :direction-anchor) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
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
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of height)))
        {direction-origin-point :real-origin
         direction-anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                               environment
                                               origin
                                               direction-anchor
                                               0
                                               90)
        pall-angle (v/normalize-angle
                    (v/angle-to-point direction-origin-point
                                      direction-anchor-point))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     band-width
                                     pall-angle)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point pall-angle unadjusted-origin-point origin-point)
                                           (chevron/mirror-point pall-angle unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-left relative-right] (chevron/arm-diagonals pall-angle origin-point anchor-point)
        diagonal-left (v/+ origin-point relative-left)
        diagonal-right (v/+ origin-point relative-right)
        angle-left (v/normalize-angle (v/angle-to-point origin-point diagonal-left))
        angle-right (v/normalize-angle (v/angle-to-point origin-point diagonal-right))
        joint-angle (v/normalize-angle (- angle-left angle-right))
        delta (/ band-width 2 (Math/sin (-> joint-angle
                                            (* Math/PI)
                                            (/ 180)
                                            (/ 2))))
        offset-lower (v/rotate
                      (v/v delta 0)
                      pall-angle)
        offset-upper (v/rotate
                      (v/v (- delta) 0)
                      pall-angle)
        corner-lower (v/+ origin-point offset-lower)
        left-upper (v/+ diagonal-left offset-upper)
        left-lower (v/+ diagonal-left offset-lower)
        right-upper (v/+ diagonal-right offset-upper)
        right-lower (v/+ diagonal-right offset-lower)
        dx (/ band-width 2)
        dy (/ band-width 2 (Math/tan (-> 180
                                         (- (/ joint-angle 2))
                                         (/ 2)
                                         (* Math/PI)
                                         (/ 180))))
        offset-three-left (v/rotate
                           (v/v (- dy) dx)
                           pall-angle)
        offset-three-right (v/rotate
                            (v/v (- dy) (- dx))
                            pall-angle)
        direction-three (v/* (v/+ relative-left relative-right) -1)
        corner-left (v/+ origin-point offset-three-left)
        corner-right (v/+ origin-point offset-three-right)
        corner-left-end (v/+ corner-left direction-three)
        corner-right-end (v/+ corner-right direction-three)
        intersection-left-upper (v/find-first-intersection-of-ray corner-left left-upper environment)
        intersection-right-upper (v/find-first-intersection-of-ray corner-right right-upper environment)
        intersection-left-lower (v/find-first-intersection-of-ray corner-lower left-lower environment)
        intersection-right-lower (v/find-first-intersection-of-ray corner-lower right-lower environment)
        intersection-left-three (v/find-first-intersection-of-ray corner-left corner-left-end environment)
        intersection-right-three (v/find-first-intersection-of-ray corner-right corner-right-end environment)
        end-left-upper (-> intersection-left-upper
                           (v/- corner-left)
                           v/abs)
        end-right-upper (-> intersection-right-upper
                            (v/- corner-right)
                            v/abs)
        end-left-lower (-> intersection-left-lower
                           (v/- corner-lower)
                           v/abs)
        end-right-lower (-> intersection-right-lower
                            (v/- corner-lower)
                            v/abs)
        end (max end-left-upper
                 end-right-upper
                 end-left-lower
                 end-right-lower)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (util/percent-of height))
                          (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        extra-line (-> extra-line
                       (update-in [:fimbriation :thickness-1] (util/percent-of height))
                       (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-right-first :line
         line-right-first-start :line-start
         :as line-right-first-data} (line/create line
                                                 corner-right corner-right-end
                                                 :reversed? true
                                                 ;; :real-start 0
                                                 ;; :real-end end
                                                 :context context
                                                 :environment environment)
        {line-right-second :line
         :as line-right-second-data} (line/create line
                                                  corner-right right-upper
                                                  ;; :real-start 0
                                                  ;; :real-end end
                                                  :context context
                                                  :environment environment)
        {line-left-first :line
         line-left-first-start :line-start
         :as line-left-first-data} (line/create opposite-line
                                                corner-left left-upper
                                                :reversed? true
                                                ;; :real-start 0
                                                ;; :real-end end
                                                :context context
                                                :environment environment)
        {line-left-second :line
         :as line-left-second-data} (line/create opposite-line
                                                 corner-left corner-left-end
                                                 ;; :real-start 0
                                                 ;; :real-end end
                                                 :context context
                                                 :environment environment)
        {line-right-lower :line
         line-right-lower-start :line-start
         :as line-right-lower-data} (line/create extra-line
                                                 corner-lower right-lower
                                                 :reversed? true
                                                 :real-start 0
                                                 :real-end end
                                                 :context context
                                                 :environment environment)
        {line-left-lower :line
         line-left-lower-start :line-start
         :as line-left-lower-data} (line/create extra-line
                                                corner-lower left-lower
                                                :real-start 0
                                                :real-end end
                                                :context context
                                                :environment environment)
        part [["M" (v/+ corner-right-end
                        line-right-first-start)
               (svg/stitch line-right-first)
               "L" corner-right
               (svg/stitch line-right-second)
               "L" (v/+ right-lower
                        line-right-lower-start)
               (svg/stitch line-right-lower)
               "L" (v/+ corner-lower
                        line-left-lower-start)
               (svg/stitch line-left-lower)
               "L" (v/+ left-upper
                        line-left-first-start)
               (svg/stitch line-left-first)
               "L" corner-left
               (svg/stitch line-left-second)
               "z"]
              [top-left bottom-right]]]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     (line/render line [line-right-first-data
                        line-right-second-data] corner-right-end outline? context)
     (line/render opposite-line [line-left-first-data
                                 line-left-second-data] left-upper outline? context)
     (line/render extra-line [line-right-lower-data
                              line-left-lower-data] right-lower outline? context)
     #_[cottising/render-chevron-cottise
        :cottise-1 :cottise-2 :cottise-1
        path environment context
        :distance-fn (fn [distance half-joint-angle-rad]
                       (-> (- distance)
                           (/ 100)
                           (* height)
                           (+ line-right-upper-min)
                           (/ (if (zero? half-joint-angle-rad)
                                0.00001
                                (Math/sin half-joint-angle-rad)))))
        :alignment :left
        :width width
        :height height
        :pall-angle pall-angle
        :joint-angle joint-angle
        :corner-point corner-upper]
     #_[cottising/render-chevron-cottise
        :cottise-opposite-1 :cottise-opposite-2 :cottise-opposite-1
        path environment context
        :distance-fn (fn [distance half-joint-angle-rad]
                       (-> (+ distance)
                           (/ 100)
                           (* height)
                           (- line-right-lower-min)
                           (/ (if (zero? half-joint-angle-rad)
                                0.00001
                                (Math/sin half-joint-angle-rad)))))
        :alignment :right
        :width width
        :height height
        :pall-angle pall-angle
        :joint-angle joint-angle
        :corner-point corner-lower
        :swap-lines? true]]))
