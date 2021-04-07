(ns heraldry.coat-of-arms.field.type.per-saltire
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.options :as division-options]
            [heraldry.coat-of-arms.field.shared :as shared]
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
        opposite-line                                (division-options/sanitize-opposite-line division line)
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
        intersection-top-left                        (v/find-first-intersection-of-ray origin-point diagonal-top-left environment)
        intersection-top-right                       (v/find-first-intersection-of-ray origin-point diagonal-top-right environment)
        intersection-bottom-left                     (v/find-first-intersection-of-ray origin-point diagonal-bottom-left environment)
        intersection-bottom-right                    (v/find-first-intersection-of-ray origin-point diagonal-bottom-right environment)
        arm-length                                   (->> [intersection-top-left
                                                           intersection-top-right
                                                           intersection-bottom-left
                                                           intersection-bottom-right]
                                                          (map #(-> %
                                                                    (v/- origin-point)
                                                                    v/abs))
                                                          (apply max))
        line                                         (-> line
                                                         (dissoc :fimbriation))
        {line-top-left       :line
         line-top-left-start :line-start}            (line/create line
                                                                   origin-point diagonal-top-left
                                                                   :reversed? true
                                                                   :real-start 0
                                                                   :real-end arm-length
                                                                   :render-options render-options
                                                                   :environment environment)
        {line-top-right       :line
         line-top-right-start :line-start}           (line/create opposite-line
                                                                   origin-point diagonal-top-right
                                                                   :flipped? true
                                                                   :real-start 0
                                                                   :real-end arm-length
                                                                   :render-options render-options
                                                                   :environment environment)
        {line-bottom-right       :line
         line-bottom-right-start :line-start}        (line/create line
                                                                   origin-point diagonal-bottom-right
                                                                   :reversed? true
                                                                   :real-start 0
                                                                   :real-end arm-length
                                                                   :render-options render-options
                                                                   :environment environment)
        {line-bottom-left       :line
         line-bottom-left-start :line-start}         (line/create opposite-line
                                                                   origin-point diagonal-bottom-left
                                                                   :flipped? true
                                                                   :real-start 0
                                                                   :real-end arm-length
                                                                   :render-options render-options
                                                                   :environment environment)
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
                                                       [intersection-top-left
                                                        intersection-top-right
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
                                                       [intersection-top-left
                                                        intersection-bottom-left
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
                                                       [intersection-top-right
                                                        intersection-bottom-right
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
                                                       [intersection-bottom-left
                                                        intersection-bottom-right
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

