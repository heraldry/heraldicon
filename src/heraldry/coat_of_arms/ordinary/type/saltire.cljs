(ns heraldry.coat-of-arms.ordinary.type.saltire
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.saltire :as saltire]
            [heraldry.vector.core :as v]
            [heraldry.interface :as interface]
            [heraldry.svg :as svg]
            [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/saltire)

(defmethod ordinary-interface/display-name ordinary-type [_] "Saltire")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        anchor (interface/get-sanitized-data (conj path :anchor) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x unadjusted-origin-point))
        bottom (assoc (:bottom points) :x (:x unadjusted-origin-point))
        left (assoc (:left points) :y (:y unadjusted-origin-point))
        right (assoc (:right points) :y (:y unadjusted-origin-point))
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of width)))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     band-width
                                     nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals origin-point anchor-point)
        diagonal-top-left (v/+ origin-point relative-top-left)
        diagonal-top-right (v/+ origin-point relative-top-right)
        diagonal-bottom-left (v/+ origin-point relative-bottom-left)
        diagonal-bottom-right (v/+ origin-point relative-bottom-right)
        angle-bottom-right (v/angle-to-point origin-point diagonal-bottom-right)
        angle (-> angle-bottom-right (* Math/PI) (/ 180))
        dx (/ band-width 2 (Math/sin angle))
        dy (/ band-width 2 (Math/cos angle))
        offset-top (v/v 0 (- dy))
        offset-bottom (v/v 0 dy)
        offset-left (v/v (- dx) 0)
        offset-right (v/v dx 0)
        corner-top (v/+ origin-point offset-top)
        corner-bottom (v/+ origin-point offset-bottom)
        corner-left (v/+ origin-point offset-left)
        corner-right (v/+ origin-point offset-right)
        top-left-upper (v/+ diagonal-top-left offset-top)
        top-left-lower (v/+ diagonal-top-left offset-bottom)
        top-right-upper (v/+ diagonal-top-right offset-top)
        top-right-lower (v/+ diagonal-top-right offset-bottom)
        bottom-left-upper (v/+ diagonal-bottom-left offset-top)
        bottom-left-lower (v/+ diagonal-bottom-left offset-bottom)
        bottom-right-upper (v/+ diagonal-bottom-right offset-top)
        bottom-right-lower (v/+ diagonal-bottom-right offset-bottom)
        intersection-top-left-upper (v/find-first-intersection-of-ray corner-top top-left-upper environment)
        intersection-top-right-upper (v/find-first-intersection-of-ray corner-top top-right-upper environment)
        intersection-top-left-lower (v/find-first-intersection-of-ray corner-left top-left-lower environment)
        intersection-top-right-lower (v/find-first-intersection-of-ray corner-right top-right-lower environment)
        intersection-bottom-left-upper (v/find-first-intersection-of-ray corner-left bottom-left-upper environment)
        intersection-bottom-right-upper (v/find-first-intersection-of-ray corner-right bottom-right-upper environment)
        intersection-bottom-left-lower (v/find-first-intersection-of-ray corner-bottom bottom-left-lower environment)
        intersection-bottom-right-lower (v/find-first-intersection-of-ray corner-bottom bottom-right-lower environment)
        end-top-left-upper (-> intersection-top-left-upper
                               (v/- corner-top)
                               v/abs)
        end-top-right-upper (-> intersection-top-right-upper
                                (v/- corner-top)
                                v/abs)
        end-top-left-lower (-> intersection-top-left-lower
                               (v/- corner-left)
                               v/abs)
        end-top-right-lower (-> intersection-top-right-lower
                                (v/- corner-right)
                                v/abs)
        end-bottom-left-upper (-> intersection-bottom-left-upper
                                  (v/- corner-left)
                                  v/abs)
        end-bottom-right-upper (-> intersection-bottom-right-upper
                                   (v/- corner-right)
                                   v/abs)
        end-bottom-left-lower (-> intersection-bottom-left-lower
                                  (v/- corner-bottom)
                                  v/abs)
        end-bottom-right-lower (-> intersection-bottom-right-lower
                                   (v/- corner-bottom)
                                   v/abs)
        end (max end-top-left-upper
                 end-top-right-upper
                 end-top-left-lower
                 end-top-right-lower
                 end-bottom-left-upper
                 end-bottom-right-upper
                 end-bottom-left-lower
                 end-bottom-right-lower)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-top-left-lower :line
         line-top-left-lower-start :line-start
         line-top-left-lower-min :line-min
         :as line-top-left-lower-data} (line/create line
                                                    corner-left top-left-lower
                                                    :real-start 0
                                                    :real-end end
                                                    :context context
                                                    :environment environment)
        {line-top-left-upper :line
         line-top-left-upper-start :line-start
         :as line-top-left-upper-data} (line/create line
                                                    corner-top top-left-upper
                                                    :reversed? true
                                                    :real-start 0
                                                    :real-end end
                                                    :context context
                                                    :environment environment)
        {line-top-right-upper :line
         line-top-right-upper-start :line-start
         :as line-top-right-upper-data} (line/create line
                                                     corner-top top-right-upper
                                                     :real-start 0
                                                     :real-end end
                                                     :context context
                                                     :environment environment)
        {line-top-right-lower :line
         line-top-right-lower-start :line-start
         :as line-top-right-lower-data} (line/create line
                                                     corner-right top-right-lower
                                                     :reversed? true
                                                     :real-start 0
                                                     :real-end end
                                                     :context context
                                                     :environment environment)
        {line-bottom-right-upper :line
         line-bottom-right-upper-start :line-start
         :as line-bottom-right-upper-data} (line/create line
                                                        corner-right bottom-right-upper
                                                        :real-start 0
                                                        :real-end end
                                                        :context context
                                                        :environment environment)
        {line-bottom-right-lower :line
         line-bottom-right-lower-start :line-start
         :as line-bottom-right-lower-data} (line/create line
                                                        corner-bottom bottom-right-lower
                                                        :reversed? true
                                                        :real-start 0
                                                        :real-end end
                                                        :context context
                                                        :environment environment)
        {line-bottom-left-lower :line
         line-bottom-left-lower-start :line-start
         :as line-bottom-left-lower-data} (line/create line
                                                       corner-bottom bottom-left-lower
                                                       :real-start 0
                                                       :real-end end
                                                       :context context
                                                       :environment environment)
        {line-bottom-left-upper :line
         line-bottom-left-upper-start :line-start
         :as line-bottom-left-upper-data} (line/create line
                                                       corner-left bottom-left-upper
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end end
                                                       :context context
                                                       :environment environment)
        part [["M" (v/+ corner-left
                        line-top-left-lower-start)
               (svg/stitch line-top-left-lower)
               "L" (v/+ top-left-upper
                        line-top-left-upper-start)
               (svg/stitch line-top-left-upper)
               "L" (v/+ corner-top
                        line-top-right-upper-start)
               (svg/stitch line-top-right-upper)
               "L" (v/+ top-right-lower
                        line-top-right-lower-start)
               (svg/stitch line-top-right-lower)
               "L" (v/+ corner-right
                        line-bottom-right-upper-start)
               (svg/stitch line-bottom-right-upper)
               "L" (v/+ bottom-right-lower
                        line-bottom-right-lower-start)
               (svg/stitch line-bottom-right-lower)
               "L" (v/+ corner-bottom
                        line-bottom-left-lower-start)
               (svg/stitch line-bottom-left-lower)
               "L" (v/+ bottom-left-upper
                        line-bottom-left-upper-start)
               (svg/stitch line-bottom-left-upper)
               "z"]
              [top bottom left right]]]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     [line/render line [line-top-left-upper-data
                        line-top-right-upper-data] top-left-upper outline? context]
     [line/render line [line-top-right-lower-data
                        line-bottom-right-upper-data] top-right-lower outline? context]
     [line/render line [line-bottom-right-lower-data
                        line-bottom-left-lower-data] bottom-right-lower outline? context]
     [line/render line [line-bottom-left-upper-data
                        line-top-left-lower-data] bottom-left-upper outline? context]
     [:<>
      (for [[chevron-angle
             corner-point
             half-joint-angle] [[270 corner-top (- 90 angle-bottom-right)]
                                [180 corner-left angle-bottom-right]
                                [0 corner-right angle-bottom-right]
                                [90 corner-bottom (- 90 angle-bottom-right)]]]
        ^{:key chevron-angle}
        [cottising/render-chevron-cottise
         :cottise-1 :cottise-2 :cottise-opposite-1
         path environment context
         :distance-fn (fn [distance half-joint-angle-rad]
                        (-> (+ distance)
                            (/ 100)
                            (* width)
                            (- line-top-left-lower-min)
                            (/ (if (zero? half-joint-angle-rad)
                                 0.00001
                                 (Math/sin half-joint-angle-rad)))))
         :alignment :right
         :width width
         :height height
         :chevron-angle chevron-angle
         :joint-angle (* 2 half-joint-angle)
         :corner-point corner-point
         :swap-lines? true])]]))
