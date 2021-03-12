(ns heraldry.coat-of-arms.division.type.per-chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
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
                 diagonal-bottom-left
                 diagonal-bottom-right]]

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
                [diagonal-bottom-left
                 origin-point
                 diagonal-bottom-right
                 bottom-left
                 bottom-right]]]
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      environment division context]
     (line/render line [line-left-data
                        line-right-data] diagonal-bottom-left outline? render-options)]))
