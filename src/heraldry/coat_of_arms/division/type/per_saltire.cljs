(ns heraldry.coat-of-arms.division.type.per-saltire
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
  {:display-name "Per saltire"
   :value        :per-saltire
   :parts        ["chief" "dexter" "sinister" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor]}                 (options/sanitize division (division-options/options division))
        points                                       (:points environment)
        top-left                                     (:top-left points)
        top-right                                    (:top-right points)
        bottom-left                                  (:bottom-left points)
        bottom-right                                 (:bottom-right points)
        {origin-point :real-origin
         anchor-point :real-anchor}                  (angle/calculate-origin-and-anchor
                                                      environment
                                                      origin
                                                      anchor
                                                      0
                                                      nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals origin-point anchor-point)
        diagonal-top-left                            (v/+ origin-point relative-top-left)
        diagonal-top-right                           (v/+ origin-point relative-top-right)
        diagonal-bottom-left                         (v/+ origin-point relative-bottom-left)
        diagonal-bottom-right                        (v/+ origin-point relative-bottom-right)
        angle-top-left                               (angle/angle-to-point origin-point diagonal-top-left)
        angle-top-right                              (angle/angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                            (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                           (angle/angle-to-point origin-point diagonal-bottom-right)
        line                                         (-> line
                                                         (dissoc :fimbriation))
        {line-top-left       :line
         line-top-left-start :line-start}            (line/create line
                                                                  (v/abs (v/- diagonal-top-left origin-point))
                                                                  :angle (+ angle-top-left 180)
                                                                  :reversed? true
                                                                  :render-options render-options)
        {line-top-right       :line
         line-top-right-start :line-start}           (line/create line
                                                                  (v/abs (v/- diagonal-top-right origin-point))
                                                                  :angle angle-top-right
                                                                  :flipped? true
                                                                  :render-options render-options)
        {line-bottom-right       :line
         line-bottom-right-start :line-start}        (line/create line
                                                                  (v/abs (v/- diagonal-bottom-right origin-point))
                                                                  :angle (+ angle-bottom-right 180)
                                                                  :reversed? true
                                                                  :render-options render-options)
        {line-bottom-left       :line
         line-bottom-left-start :line-start}         (line/create line
                                                                  (v/abs (v/- diagonal-bottom-left origin-point))
                                                                  :angle angle-bottom-left
                                                                  :flipped? true
                                                                  :render-options render-options)
        ;; TODO: sub fields need better environment determination, especially with an adjusted origin,
        ;; the resulting environments won't be very well centered
        parts                                        [[["M" (v/+ diagonal-top-left
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
                                                       [top-left
                                                        top-right
                                                        origin-point]]

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
                                                       [top-left
                                                        bottom-left
                                                        origin-point]]

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
                                                       [top-right
                                                        bottom-right
                                                        origin-point]]

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
                                                       [bottom-left
                                                        bottom-right
                                                        origin-point]]]]

    [:<>
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
      environment division context]
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
                     (svg/stitch line-bottom-left)])}]])]))
