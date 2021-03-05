(ns heraldry.coat-of-arms.division.type.tierced-per-pairle
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
  {:display-name "Tierced per pairle"
   :value        :tierced-per-pairle
   :parts        ["chief" "dexter" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]}      (options/sanitize division (division-options/options division))
        points                                   (:points environment)
        origin-point                             (position/calculate origin environment :fess)
        bottom                                   (assoc (:bottom points) :x (:x origin-point))
        bottom-left                              (:bottom-left points)
        bottom-right                             (:bottom-right points)
        left                                     (assoc (:left points) :y (:y origin-point))
        right                                    (assoc (:right points) :y (:y origin-point))
        direction                                (angle/direction diagonal-mode points origin-point)
        diagonal-top-left                        (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                       (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle-top-left                           (angle/angle-to-point origin-point diagonal-top-left)
        angle-top-right                          (angle/angle-to-point origin-point diagonal-top-right)
        {line-top-left       :line
         line-top-left-start :line-start}        (line/create line
                                                              (v/abs (v/- diagonal-top-left origin-point))
                                                              :angle (+ angle-top-left 180)
                                                              :reversed? true
                                                              :render-options render-options)
        {line-top-right       :line
         line-top-right-start :line-start}       (line/create line
                                                              (v/abs (v/- diagonal-top-right origin-point))
                                                              :angle angle-top-right
                                                              :flipped? true
                                                              :render-options render-options)
        {line-bottom       :line
         line-bottom-start :line-start}          (line/create line
                                                              (v/abs (v/- bottom origin-point))
                                                              :flipped? true
                                                              :angle 90
                                                              :render-options render-options)
        {line-bottom-reversed       :line
         line-bottom-reversed-start :line-start} (line/create line
                                                              (v/abs (v/- bottom origin-point))
                                                              :angle -90
                                                              :reversed? true
                                                              :render-options render-options)
        parts                                    [[["M" (v/+ diagonal-top-left
                                                             line-top-left-start)
                                                    (svg/stitch line-top-left)
                                                    "L" origin-point
                                                    (svg/stitch line-top-right)
                                                    (infinity/path :counter-clockwise
                                                                   [:right :left]
                                                                   [(v/+ diagonal-top-right
                                                                         line-top-right-start)
                                                                    (v/+ diagonal-top-left
                                                                         line-top-left-start)])
                                                    "z"]
                                                   [diagonal-top-left
                                                    origin-point
                                                    diagonal-top-right]]

                                                  [["M" (v/+ bottom
                                                             line-bottom-reversed-start)
                                                    (svg/stitch line-bottom-reversed)
                                                    "L" origin-point
                                                    (svg/stitch line-top-right)
                                                    (infinity/path :clockwise
                                                                   [:right :bottom]
                                                                   [(v/+ diagonal-top-right
                                                                         line-top-right-start)
                                                                    (v/+ bottom
                                                                         line-bottom-reversed-start)])
                                                    "z"]
                                                   [origin-point
                                                    diagonal-top-right
                                                    bottom
                                                    diagonal-top-right
                                                    bottom-right]]

                                                  [["M" (v/+ diagonal-top-left
                                                             line-top-left-start)
                                                    (svg/stitch line-top-left)
                                                    "L" origin-point
                                                    (svg/stitch line-bottom)
                                                    (infinity/path :clockwise
                                                                   [:bottom :left]
                                                                   [(v/+ bottom
                                                                         line-bottom-start)
                                                                    (v/+ diagonal-top-left
                                                                         line-top-left-start)])
                                                    "z"]
                                                   [origin-point
                                                    bottom-left
                                                    bottom
                                                    diagonal-top-left]]]]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all
       [(svg/make-path
         ["M" (v/+ bottom
                   line-bottom-reversed-start)
          (svg/stitch line-bottom-reversed)])]
       nil]
      environment division context]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-top-left
                              line-top-left-start)
                     (svg/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-bottom)])}]])]))
