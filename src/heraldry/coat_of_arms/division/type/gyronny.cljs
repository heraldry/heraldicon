(ns heraldry.coat-of-arms.division.type.gyronny
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.shared.saltire :as saltire]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Gyronny"
   :value :gyronny
   :parts ["I" "II" "III" "IV" "V" "VI" "VII" "VIII"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor]} (options/sanitize division (division-options/options division))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        top (assoc (:top points) :x (:x origin-point))
        bottom (assoc (:bottom points) :x (:x origin-point))
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals origin-point anchor-point)
        diagonal-top-left (v/+ origin-point relative-top-left)
        diagonal-top-right (v/+ origin-point relative-top-right)
        diagonal-bottom-left (v/+ origin-point relative-bottom-left)
        diagonal-bottom-right (v/+ origin-point relative-bottom-right)
        angle-top-left (angle/angle-to-point origin-point diagonal-top-left)
        angle-top-right (angle/angle-to-point origin-point diagonal-top-right)
        angle-bottom-left (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right (angle/angle-to-point origin-point diagonal-bottom-right)
        line (-> line
                 (dissoc :fimbriation))
        {line-top :line
         line-top-start :line-start} (line/create line
                                                  (v/abs (v/- top origin-point))
                                                  :angle 90
                                                  :reversed? true
                                                  :render-options render-options)
        {line-right :line
         line-right-start :line-start} (line/create line
                                                    (v/abs (v/- right origin-point))
                                                    :reversed? true
                                                    :angle 180
                                                    :render-options render-options)
        {line-bottom :line
         line-bottom-start :line-start} (line/create line
                                                     (v/abs (v/- bottom origin-point))
                                                     :angle -90
                                                     :reversed? true
                                                     :render-options render-options)
        {line-left :line
         line-left-start :line-start} (line/create line
                                                   (v/abs (v/- left origin-point))
                                                   :reversed? true
                                                   :render-options render-options)
        {line-top-left :line} (line/create line
                                           (v/abs (v/- diagonal-top-left origin-point))
                                           :flipped? true
                                           :angle angle-top-left
                                           :render-options render-options)
        {line-top-right :line} (line/create line
                                            (v/abs (v/- diagonal-top-right origin-point))
                                            :flipped? true
                                            :angle angle-top-right
                                            :render-options render-options)
        {line-bottom-right :line} (line/create line
                                               (v/abs (v/- diagonal-bottom-right origin-point))
                                               :flipped? true
                                               :angle angle-bottom-right
                                               :render-options render-options)
        {line-bottom-left :line} (line/create line
                                              (v/abs (v/- diagonal-bottom-left origin-point))
                                              :flipped? true
                                              :angle angle-bottom-left
                                              :render-options render-options)
        parts [[["M" (v/+ top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-top-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [diagonal-top-left
                                 (v/+ top
                                      line-top-start)])
                 "z"]
                [top-left
                 origin-point
                 top]]

               [["M" (v/+ top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [diagonal-top-right
                                 (v/+ top
                                      line-top-start)])
                 "z"]
                [top
                 origin-point
                 top-right]]

               [["M" (v/+ left
                          line-left-start)
                 (svg/stitch line-left)
                 "L" origin-point
                 (svg/stitch line-top-left)
                 (infinity/path :counter-clockwise
                                [:left :left]
                                [diagonal-top-left
                                 (v/+ left
                                      line-left-start)])
                 "z"]
                [left
                 origin-point
                 top-left]]

               [["M" (v/+ right
                          line-right-start)
                 (svg/stitch line-right)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :clockwise
                                [:right :right]
                                [diagonal-top-right
                                 (v/+ right
                                      line-right-start)])
                 "z"]
                [top-right
                 origin-point
                 right]]

               [["M" (v/+ left
                          line-left-start)
                 (svg/stitch line-left)
                 "L" origin-point
                 (svg/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:left :left]
                                [diagonal-bottom-left
                                 (v/+ left
                                      line-left-start)])
                 "z"]
                [bottom-left
                 origin-point
                 left]]

               [["M" (v/+ right
                          line-right-start)
                 (svg/stitch line-right)
                 "L" origin-point
                 (svg/stitch line-bottom-right)
                 (infinity/path :counter-clockwise
                                [:right :right]
                                [diagonal-bottom-right
                                 (v/+ right
                                      line-right-start)])
                 "z"]
                [right
                 origin-point
                 bottom-right]]

               [["M" (v/+ bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [diagonal-bottom-left
                                 (v/+ bottom
                                      line-bottom-start)])
                 "z"]
                [bottom
                 origin-point
                 bottom-left]]

               [["M" (v/+ bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-bottom-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [diagonal-bottom-right
                                 (v/+ bottom
                                      line-bottom-start)])
                 "z"]
                [bottom-right
                 origin-point
                 bottom]]]]

    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all
       [(svg/make-path
         ["M" origin-point
          (svg/stitch line-top-right)])]
       [(svg/make-path
         ["M" (v/+ left
                   line-left-start)
          (svg/stitch line-left)])]
       [(svg/make-path
         ["M" (v/+ right
                   line-right-start)
          (svg/stitch line-right)])]
       [(svg/make-path
         ["M" origin-point
          (svg/stitch line-bottom-left)])]
       [(svg/make-path
         ["M" origin-point
          (svg/stitch line-bottom-right)])]
       [(svg/make-path
         ["M" (v/+ bottom
                   line-bottom-start)
          (svg/stitch line-bottom)])]
       nil]
      environment division context]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ top
                              line-top-start)
                     (svg/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ right
                              line-right-start)
                     (svg/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ bottom
                              line-bottom-start)
                     (svg/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ left
                              line-left-start)
                     (svg/stitch line-left)])}]])]))
