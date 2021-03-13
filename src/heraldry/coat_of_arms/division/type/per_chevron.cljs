(ns heraldry.coat-of-arms.division.type.per-chevron
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
        diagonal-bottom-left (v/+ origin-point relative-left)
        diagonal-bottom-right (v/+ origin-point relative-right)
        angle-bottom-left (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right (angle/angle-to-point origin-point diagonal-bottom-right)
        {line-left :line
         line-left-start :line-start
         :as line-left-data} (line/create line
                                          (v/abs (v/- diagonal-bottom-left origin-point))
                                          :angle (+ angle-bottom-left 180)
                                          :reversed? true
                                          :render-options render-options)
        {line-right :line
         line-right-end :line-end
         :as line-right-data} (line/create line
                                           (v/abs (v/- diagonal-bottom-right origin-point))
                                           :angle angle-bottom-right
                                           :render-options render-options)
        infinity-points (case variant
                          :chief [:left :right]
                          :dexter [:bottom :top]
                          :sinister [:top :bottom]
                          [:right :left])
        parts [[["M" (v/+ diagonal-bottom-left
                          line-left-start)
                 (svg/stitch line-left)
                 (svg/stitch line-right)
                 (infinity/path :counter-clockwise
                                infinity-points
                                [(v/+ diagonal-bottom-right
                                      line-right-end)
                                 (v/+ diagonal-bottom-left
                                      line-left-start)])
                 "z"]
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/+ diagonal-bottom-left
                          line-left-start)
                 (svg/stitch line-left)
                 (svg/stitch line-right)
                 (infinity/path :clockwise
                                infinity-points
                                [(v/+ diagonal-bottom-right
                                      line-right-end)
                                 (v/+ diagonal-bottom-left
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
                        line-right-data] diagonal-bottom-left outline? render-options)]))
