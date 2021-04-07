(ns heraldry.coat-of-arms.field.type.per-chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per chevron"
   :value :per-chevron
   ;; TODO: this naming now depends on the variant
   :parts ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                variant]} (options/sanitize division (division-options/options division))
        opposite-line (division-options/sanitize-opposite-line division line)
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        anchor (chevron/sanitize-anchor variant anchor)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
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
        intersection-left (v/find-first-intersection-of-ray origin-point diagonal-left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point diagonal-right environment)
        end-left (-> intersection-left
                     (v/- origin-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/- origin-point)
                      v/abs)
        end (max end-left end-right)
        {line-left :line
         line-left-start :line-start
         :as line-left-data} (line/create line
                                          origin-point diagonal-left
                                          :real-start 0
                                          :real-end end
                                          :reversed? true
                                          :render-options render-options
                                          :environment environment)
        {line-right :line
         line-right-end :line-end
         :as line-right-data} (line/create opposite-line
                                           origin-point diagonal-right
                                           :real-start 0
                                           :real-end end
                                           :render-options render-options
                                           :environment environment)
        infinity-points (case variant
                          :chief [:left :right]
                          :dexter [:bottom :top]
                          :sinister [:top :bottom]
                          [:right :left])
        parts [[["M" (v/+ diagonal-left
                          line-left-start)
                 (svg/stitch line-left)
                 (svg/stitch line-right)
                 (infinity/path :counter-clockwise
                                infinity-points
                                [(v/+ diagonal-right
                                      line-right-end)
                                 (v/+ diagonal-left
                                      line-left-start)])
                 "z"]
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/+ diagonal-left
                          line-left-start)
                 (svg/stitch line-left)
                 (svg/stitch line-right)
                 (infinity/path :clockwise
                                infinity-points
                                [(v/+ diagonal-right
                                      line-right-end)
                                 (v/+ diagonal-left
                                      line-left-start)])
                 "z"]
                (-> [origin-point]
                    (concat (case variant
                              :chief [top-left top-right]
                              :dexter [top-left bottom-left]
                              :sinister [top-right bottom-right]
                              [bottom-left bottom-right]))
                    vec)]]
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      environment division context]
     (line/render line [line-left-data
                        line-right-data] diagonal-left outline? render-options)]))
