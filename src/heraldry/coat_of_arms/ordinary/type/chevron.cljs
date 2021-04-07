(ns heraldry.coat-of-arms.ordinary.type.chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Chevron"
   :value :chevron}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                variant geometry]} (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]} geometry
        opposite-line (ordinary-options/sanitize-opposite-line ordinary line)
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of height)))
        anchor (chevron/sanitize-anchor variant anchor)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     band-width
                                     (case variant
                                       :base 90
                                       :chief -90
                                       :dexter 180
                                       0))
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point variant unadjusted-origin-point origin-point)
                                           (chevron/mirror-point variant unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-left relative-right] (chevron/arm-diagonals variant origin-point anchor-point)
        diagonal-left (v/+ origin-point relative-left)
        diagonal-right (v/+ origin-point relative-right)
        angle-left (v/angle-to-point origin-point diagonal-left)
        angle-right (v/angle-to-point origin-point diagonal-right)
        joint-angle (- angle-left angle-right)
        middle-angle (-> (/ (+ angle-left angle-right) 2)
                         (* Math/PI) (/ 180))
        delta (/ band-width 2 (Math/sin (-> joint-angle
                                            (* Math/PI)
                                            (/ 180)
                                            (/ 2))))
        offset-lower (v/v (* (Math/cos middle-angle)
                             delta)
                          (* (Math/sin middle-angle)
                             delta))
        offset-upper (v/v (* (Math/cos middle-angle)
                             (- delta))
                          (* (Math/sin middle-angle)
                             (- delta)))
        corner-upper (v/+ origin-point offset-upper)
        corner-lower (v/+ origin-point offset-lower)
        left-upper (v/+ diagonal-left offset-upper)
        left-lower (v/+ diagonal-left offset-lower)
        right-upper (v/+ diagonal-right offset-upper)
        right-lower (v/+ diagonal-right offset-lower)
        intersection-left-upper (v/find-first-intersection-of-ray corner-upper left-upper environment)
        intersection-right-upper (v/find-first-intersection-of-ray corner-upper left-upper environment)
        intersection-left-lower (v/find-first-intersection-of-ray corner-lower left-lower environment)
        intersection-right-lower (v/find-first-intersection-of-ray corner-lower left-lower environment)
        end-left-upper (-> intersection-left-upper
                           (v/- corner-upper)
                           v/abs)
        end-right-upper (-> intersection-right-upper
                            (v/- corner-upper)
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
        {line-right-upper :line
         line-right-upper-start :line-start
         :as line-right-upper-data} (line/create line
                                                 corner-upper right-upper
                                                 :real-start 0
                                                 :real-end end
                                                 :render-options render-options
                                                 :environment environment)
        {line-right-lower :line
         line-right-lower-start :line-start
         :as line-right-lower-data} (line/create opposite-line
                                                 corner-lower right-lower
                                                 :reversed? true
                                                 :real-start 0
                                                 :real-end end
                                                 :render-options render-options
                                                 :environment environment)
        {line-left-lower :line
         line-left-lower-start :line-start
         :as line-left-lower-data} (line/create opposite-line
                                                corner-lower left-lower
                                                :real-start 0
                                                :real-end end
                                                :render-options render-options
                                                :environment environment)
        {line-left-upper :line
         line-left-upper-start :line-start
         :as line-left-upper-data} (line/create line
                                                corner-upper left-upper
                                                :reversed? true
                                                :real-start 0
                                                :real-end end
                                                :render-options render-options
                                                :environment environment)
        parts [[["M" (v/+ corner-upper
                          line-right-upper-start)
                 (svg/stitch line-right-upper)
                 "L" (v/+ right-lower
                          line-right-lower-start)
                 (svg/stitch line-right-lower)
                 "L" (v/+ corner-lower
                          line-left-lower-start)
                 (svg/stitch line-left-lower)
                 "L" (v/+ left-upper
                          line-left-upper-start)
                 (svg/stitch line-left-upper)
                 "z"]
                (-> [corner-upper corner-lower]
                    (concat (case variant
                              :chief [top-left top-right]
                              :dexter [top-left bottom-left]
                              :sinister [top-right bottom-right]
                              [bottom-left bottom-right]))
                    vec)]]
        field (if (:counterchanged? field)
                (counterchange/counterchange-field ordinary parent)
                field)
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [field-shared/make-subfields
      :ordinary-pale [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-left-upper-data
                        line-right-upper-data] left-upper outline? render-options)
     (line/render opposite-line [line-right-lower-data
                                 line-left-lower-data] right-lower outline? render-options)]))
