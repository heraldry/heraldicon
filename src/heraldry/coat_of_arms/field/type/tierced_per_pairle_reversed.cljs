(ns heraldry.coat-of-arms.field.type.tierced-per-pairle-reversed
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Tierced per pairle reversed"
   :value :tierced-per-pairle-reversed
   :parts ["dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor]} (options/sanitize division (field-options/options division))
        opposite-line (field-options/sanitize-opposite-line division line)
        extra-line (field-options/sanitize-extra-line division line)
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     0)
        top (assoc (:top points) :x (:x origin-point))
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point :base unadjusted-origin-point origin-point)
                                           (chevron/mirror-point :base unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-left relative-right] (chevron/arm-diagonals :base origin-point anchor-point)
        diagonal-bottom-left (v/+ origin-point relative-left)
        diagonal-bottom-right (v/+ origin-point relative-right)
        intersection-left (v/find-first-intersection-of-ray origin-point diagonal-bottom-left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point diagonal-bottom-right environment)
        end-left (-> intersection-left
                     (v/- origin-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/- origin-point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update :offset max 0))
        {line-bottom-right :line
         line-bottom-right-start :line-start} (line/create line
                                                           origin-point diagonal-bottom-right
                                                           :reversed? true
                                                           :real-start 0
                                                           :real-end end
                                                           :render-options render-options
                                                           :environment environment)
        {line-bottom-left :line
         line-bottom-left-start :line-start} (line/create opposite-line
                                                          origin-point diagonal-bottom-left
                                                          :flipped? true
                                                          :real-start 0
                                                          :real-end end
                                                          :render-options render-options
                                                          :environment environment)
        {line-top :line
         line-top-start :line-start} (line/create extra-line
                                                  origin-point top
                                                  :flipped? true
                                                  :render-options render-options
                                                  :environment environment)
        {line-top-reversed :line
         line-top-reversed-start :line-start} (line/create extra-line
                                                           origin-point top
                                                           :reversed? true
                                                           :render-options render-options
                                                           :environment environment)
        parts [[["M" (v/+ top
                          line-top-reversed-start)
                 (svg/stitch line-top-reversed)
                 "L" origin-point
                 (svg/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [(v/+ diagonal-bottom-left
                                      line-bottom-left-start)
                                 (v/+ top
                                      line-top-reversed-start)])
                 "z"]
                [top-left
                 origin-point
                 bottom-left
                 top]]

               [["M" (v/+ diagonal-bottom-right
                          line-bottom-right-start)
                 (svg/stitch line-bottom-right)
                 "L" origin-point
                 (svg/stitch line-top)
                 (infinity/path :clockwise
                                [:top :right]
                                [(v/+ top
                                      line-top-start)
                                 (v/+ diagonal-bottom-right
                                      line-bottom-right-start)])
                 "z"]
                [top-right
                 origin-point
                 top
                 bottom-right]]

               [["M" (v/+ diagonal-bottom-right
                          line-bottom-right-start)
                 (svg/stitch line-bottom-right)
                 "L" origin-point
                 (svg/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:left :right]
                                [(v/+ diagonal-bottom-left
                                      line-bottom-left-start)
                                 (v/+ diagonal-bottom-right
                                      line-bottom-right-start)])
                 "z"]
                [origin-point bottom-left bottom-right]]]]
    [:<>
     [shared/make-subfields
      (shared/field-context-key type) fields parts
      [:all
       [(svg/make-path
         ["M" (v/+ diagonal-bottom-right
                   line-bottom-right-start)
          (svg/stitch line-bottom-right)])]
       nil]
      environment division context]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-bottom-right
                              line-bottom-right-start)
                     (svg/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-bottom-left)])}]])]))
