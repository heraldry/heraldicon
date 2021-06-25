(ns heraldry.coat-of-arms.field.type.per-chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per chevron"
   :value :heraldry.field.type/per-chevron
   ;; TODO: this naming now depends on the angle of the chevron
   :parts ["chief" "base"]}
  [{:keys [type fields] :as field} environment {:keys [render-options] :as context}]
  (let [field-options (field-options/options field)
        {:keys [line origin anchor
                direction-anchor outline?]} (options/sanitize field field-options)
        raw-direction-anchor (:direction-anchor field)
        direction-anchor (options/sanitize (cond-> raw-direction-anchor
                                             (-> direction-anchor
                                                 :point
                                                 #{:left
                                                   :right
                                                   :top
                                                   :bottom}) (->
                                                              (update :offset-x #(or % (:offset-x origin)))
                                                              (update :offset-y #(or % (:offset-y origin)))))
                                           (:direction-anchor field-options))
        opposite-line (field-options/sanitize-opposite-line field line)
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        {direction-origin-point :real-origin
         direction-anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                               environment
                                               origin
                                               direction-anchor
                                               0
                                               90)
        chevron-angle (v/normalize-angle
                       (v/angle-to-point direction-origin-point
                                         direction-anchor-point))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     chevron-angle)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point chevron-angle unadjusted-origin-point origin-point)
                                           (chevron/mirror-point chevron-angle unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle origin-point anchor-point)
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
        infinity-points (cond
                          (<= 45 chevron-angle 135) [:right :left]
                          (<= 225 chevron-angle 315) [:left :right]
                          (<= 135 chevron-angle 225) [:bottom :top]
                          :else [:top :bottom])
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
                [top-left bottom-right]]]
        outline? (or (:outline? render-options)
                     outline?)]
    [:<>
     [shared/make-subfields
      (shared/field-context-key type) fields parts
      [:all nil]
      environment field context]
     (line/render line [line-left-data
                        line-right-data] diagonal-left outline? render-options)]))
