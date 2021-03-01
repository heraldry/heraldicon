(ns heraldry.coat-of-arms.division.type.per-saltire
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
  {:display-name "Per saltire"
   :value :per-saltire
   :parts ["chief" "dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode]} (options/sanitize division (division-options/options division))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top (:top points)
        bottom (:bottom points)
        left (:left points)
        right (:right points)
        direction (angle/direction diagonal-mode points origin-point)
        diagonal-top-left (v/project-x origin-point (v/dot direction (v/v -1 -1)) (-> left :x (- 50)))
        diagonal-top-right (v/project-x origin-point (v/dot direction (v/v 1 -1)) (-> right :x (+ 50)))
        diagonal-bottom-left (v/project-x origin-point (v/dot direction (v/v -1 1)) (-> left :x (- 50)))
        diagonal-bottom-right (v/project-x origin-point (v/dot direction (v/v 1 1)) (-> right :x (+ 50)))
        angle-top-left (angle/angle-to-point origin-point diagonal-top-left)
        angle-top-right (angle/angle-to-point origin-point diagonal-top-right)
        angle-bottom-left (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right (angle/angle-to-point origin-point diagonal-bottom-right)
        line (-> line
                 (dissoc :fimbriation))
        {line-top-left :line
         line-top-left-start :line-start} (line/create line
                                                       (v/abs (v/- diagonal-top-left origin-point))
                                                       :angle (+ angle-top-left 180)
                                                       :reversed? true
                                                       :render-options render-options)
        {line-top-right :line
         line-top-right-start :line-start} (line/create line
                                                        (v/abs (v/- diagonal-top-right origin-point))
                                                        :angle angle-top-right
                                                        :flipped? true
                                                        :render-options render-options)
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
        parts [[["M" (v/+ diagonal-top-left
                          line-top-left-start)
                 (svg/stitch line-top-left)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/+ diagonal-top-right
                                      line-top-left-start)
                                 (v/+ diagonal-top-left
                                      line-top-left-start)])
                 "z"]
                [top
                 (v/+ diagonal-top-right
                      line-top-right-start)
                 origin-point
                 (v/+ diagonal-top-left
                      line-top-left-start)]]

               [["M" (v/+ diagonal-top-left
                          line-top-left-start)
                 (svg/stitch line-top-left)
                 "L" origin-point
                 (svg/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:left :left]
                                [(v/+ diagonal-bottom-left
                                      line-bottom-left-start)
                                 (v/+ diagonal-top-left
                                      line-top-left-start)])
                 "z"]
                [left
                 (v/+ diagonal-bottom-left
                      line-bottom-left-start)
                 origin-point
                 (v/+ diagonal-top-left
                      line-top-left-start)]]

               [["M" (v/+ diagonal-bottom-right
                          line-bottom-right-start)
                 (svg/stitch line-bottom-right)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/+ diagonal-top-right
                                      line-top-right-start)
                                 (v/+ diagonal-bottom-right
                                      line-bottom-right-start)])
                 "z"]
                [right
                 (v/+ diagonal-top-right
                      line-top-right-start)
                 origin-point
                 (v/+ diagonal-bottom-right
                      line-bottom-right-start)]]

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
                [bottom
                 (v/+ diagonal-bottom-left
                      line-bottom-left-start)
                 origin-point
                 (v/+ diagonal-bottom-right
                      line-bottom-right-start)]]]]

    [shared/make-division
     (shared/division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" (v/+ origin-point
                  line-bottom-left-start)
         (svg/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" (v/+ diagonal-bottom-right
                  line-bottom-right-start)
         (svg/stitch line-bottom-right)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-top-left
                              line-top-left-start)
                     (svg/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ origin-point
                              line-top-right-start)
                     (svg/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ diagonal-bottom-right
                              line-bottom-right-start)
                     (svg/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ origin-point
                              line-bottom-left-start)
                     (svg/stitch line-bottom-left)])}]])
     environment division context]))
