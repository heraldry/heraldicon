(ns heraldry.coat-of-arms.field.type.per-saltire
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.shared.saltire :as saltire]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(def field-type
  :heraldry.field.type/per-saltire)

(defmethod interface/display-name field-type [_] "Per saltire")

(defmethod interface/part-names field-type [_] ["chief" "dexter" "sinister" "base"])

(defmethod interface/render-field field-type
  [path environment context]
  (let [line (options/sanitized-value (conj path :line) context)
        opposite-line (options/sanitized-value (conj path :opposite-line) context)
        origin (options/sanitized-value (conj path :origin) context)
        anchor (options/sanitized-value (conj path :anchor) context)
        outline? (or (options/render-option :outline? context)
                     (options/sanitized-value (conj path :outline?) context))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals origin-point anchor-point)
        diagonal-top-left (v/+ origin-point relative-top-left)
        diagonal-top-right (v/+ origin-point relative-top-right)
        diagonal-bottom-left (v/+ origin-point relative-bottom-left)
        diagonal-bottom-right (v/+ origin-point relative-bottom-right)
        intersection-top-left (v/find-first-intersection-of-ray origin-point diagonal-top-left environment)
        intersection-top-right (v/find-first-intersection-of-ray origin-point diagonal-top-right environment)
        intersection-bottom-left (v/find-first-intersection-of-ray origin-point diagonal-bottom-left environment)
        intersection-bottom-right (v/find-first-intersection-of-ray origin-point diagonal-bottom-right environment)
        arm-length (->> [intersection-top-left
                         intersection-top-right
                         intersection-bottom-left
                         intersection-bottom-right]
                        (map #(-> %
                                  (v/- origin-point)
                                  v/abs))
                        (apply max))
        line (-> line
                 (dissoc :fimbriation))
        {line-top-left :line
         line-top-left-start :line-start} (line/create line
                                                       origin-point diagonal-top-left
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end arm-length
                                                       :context context
                                                       :environment environment)
        {line-top-right :line
         line-top-right-start :line-start} (line/create opposite-line
                                                        origin-point diagonal-top-right
                                                        :flipped? true
                                                        :mirrored? true
                                                        :real-start 0
                                                        :real-end arm-length
                                                        :context context
                                                        :environment environment)
        {line-bottom-right :line
         line-bottom-right-start :line-start} (line/create line
                                                           origin-point diagonal-bottom-right
                                                           :reversed? true
                                                           :real-start 0
                                                           :real-end arm-length
                                                           :context context
                                                           :environment environment)
        {line-bottom-left :line
         line-bottom-left-start :line-start} (line/create opposite-line
                                                          origin-point diagonal-bottom-left
                                                          :flipped? true
                                                          :mirrored? true
                                                          :real-start 0
                                                          :real-end arm-length
                                                          :context context
                                                          :environment environment)
        ;; TODO: sub fields need better environment determination, especially with an adjusted origin,
        ;; the resulting environments won't be very well centered
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
     [shared/make-subfields2
      path parts
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
      environment context]
     (when outline?
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
