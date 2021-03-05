(ns heraldry.coat-of-arms.division.type.tierced-per-pairle-reversed
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Tierced per pairle reversed"
   :value :tierced-per-pairle-reversed
   :parts ["dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (division-options/options division))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x origin-point))
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        direction (angle/direction diagonal-mode points origin-point)
        diagonal-bottom-left (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right (angle/angle-to-point origin-point diagonal-bottom-right)
        line (-> line
                 (update :offset max 0))
        {line-bottom-right :line
         line-bottom-right-start :line-start} (line/create line
                                                           (v/abs (v/- diagonal-bottom-right origin-point))
                                                           :angle (+ angle-bottom-right 180)
                                                           :reversed? true
                                                           :render-options render-options)
        {line-bottom-left :line
         line-bottom-left-start :line-start} (line/create line
                                                          (v/abs (v/- diagonal-bottom-left origin-point))
                                                          :angle angle-bottom-left
                                                          :flipped? true
                                                          :render-options render-options)
        {line-top :line
         line-top-start :line-start} (line/create line
                                                  (v/abs (v/- top origin-point))
                                                  :flipped? true
                                                  :angle -90
                                                  :render-options render-options)
        {line-top-reversed :line
         line-top-reversed-start :line-start} (line/create line
                                                           (v/abs (v/- top origin-point))
                                                           :angle 90
                                                           :reversed? true
                                                           :render-options render-options)
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
                 diagonal-bottom-left
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
                 diagonal-bottom-right]]

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
     [shared/make-division
      (shared/division-context-key type) fields parts
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
