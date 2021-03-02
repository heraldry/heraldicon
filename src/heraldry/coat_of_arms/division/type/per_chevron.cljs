(ns heraldry.coat-of-arms.division.type.per-chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per chevron"
   :value :per-chevron
   :parts ["chief" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (division-options/options division))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        left (:left points)
        right (:right points)
        direction (angle/direction diagonal-mode points origin-point)
        diagonal-bottom-left (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right (angle/angle-to-point origin-point diagonal-bottom-right)
        joint-angle (- angle-bottom-left angle-bottom-right)
        {line-left :line
         line-left-start :line-start
         :as line-left-data} (line/create line
                                          (v/abs (v/- diagonal-bottom-left origin-point))
                                          :angle (+ angle-bottom-left 180)
                                          :joint-angle (- joint-angle)
                                          :reversed? true
                                          :render-options render-options)
        {line-right :line
         line-right-start :line-start
         line-right-end :line-end
         :as line-right-data} (line/create line
                                           (v/abs (v/- diagonal-bottom-right origin-point))
                                           :angle angle-bottom-right
                                           :joint-angle (- joint-angle)
                                           :render-options render-options)
        parts [[["M" (v/+ diagonal-bottom-left
                          line-left-start)
                 (svg/stitch line-left)
                 (svg/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/+ diagonal-bottom-right
                                      line-right-end)
                                 (v/+ diagonal-bottom-left
                                      line-left-start)])
                 "z"]
                [top-left
                 top-right
                 (v/+ diagonal-bottom-left
                      line-left-start)
                 (v/+ diagonal-bottom-right
                      line-left-start)]]

               [["M" (v/+ diagonal-bottom-left
                          line-left-start)
                 (svg/stitch line-left)
                 (svg/stitch line-right)
                 (infinity/path :clockwise
                                [:right :left]
                                [(v/+ diagonal-bottom-right
                                      line-right-end)
                                 (v/+ diagonal-bottom-left
                                      line-left-start)])
                 "z"]
                [(v/+ diagonal-bottom-left
                      line-left-start)
                 origin-point
                 (v/+ diagonal-bottom-right
                      line-right-end)
                 bottom-left
                 bottom-right]]]
        [fimbriation-elements
         fimbriation-outlines] (fimbriation/render
                                [diagonal-bottom-left :left]
                                [diagonal-bottom-right :right]
                                [line-left-data
                                 line-right-data]
                                (:fimbriation line)
                                render-options)]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      environment division context]
     fimbriation-elements
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-bottom-left
                              line-left-start)
                     (svg/stitch line-left)
                     "L" (v/+ origin-point
                              line-right-start)
                     (svg/stitch line-right)])}]
        fimbriation-outlines])]))
