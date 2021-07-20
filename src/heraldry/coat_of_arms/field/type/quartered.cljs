(ns heraldry.coat-of-arms.field.type.quartered
  (:require [heraldry.coat-of-arms.field.interface :as interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(def field-type
  :heraldry.field.type/quartered)

(defmethod interface/display-name field-type [_] "Quartered")

(defmethod interface/part-names field-type [_] ["I" "II" "III" "IV"])

(defmethod interface/render-field field-type
  [path environment context]
  (let [line (options/sanitized-value (conj path :line) context)
        opposite-line (options/sanitized-value (conj path :opposite-line) context)
        origin (options/sanitized-value (conj path :origin) context)
        outline? (or (options/render-option :outline? context)
                     (options/sanitized-value (conj path :outline?) context))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x origin-point))
        top-left (:top-left points)
        top-right (:top-right points)
        bottom (assoc (:bottom points) :x (:x origin-point))
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        intersection-top (v/find-first-intersection-of-ray origin-point top environment)
        intersection-bottom (v/find-first-intersection-of-ray origin-point bottom environment)
        intersection-left (v/find-first-intersection-of-ray origin-point left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point right environment)
        arm-length (->> [intersection-top
                         intersection-bottom
                         intersection-left
                         intersection-right]
                        (map #(-> %
                                  (v/- origin-point)
                                  v/abs))
                        (apply max))
        full-arm-length (+ arm-length 30)
        point-top (-> (v/v 0 -1)
                      (v/* full-arm-length)
                      (v/+ origin-point))
        point-bottom (-> (v/v 0 1)
                         (v/* full-arm-length)
                         (v/+ origin-point))
        point-left (-> (v/v -1 0)
                       (v/* full-arm-length)
                       (v/+ origin-point))
        point-right (-> (v/v 1 0)
                        (v/* full-arm-length)
                        (v/+ origin-point))
        line (-> line
                 (dissoc :fimbriation))
        {line-top :line
         line-top-start :line-start} (line/create line
                                                  origin-point point-top
                                                  :reversed? true
                                                  :real-start 0
                                                  :real-end arm-length
                                                  :context context
                                                  :environment environment)
        {line-right :line
         line-right-start :line-start} (line/create opposite-line
                                                    origin-point point-right
                                                    :flipped? true
                                                    :mirrored? true
                                                    :real-start 0
                                                    :real-end arm-length
                                                    :context context
                                                    :environment environment)
        {line-bottom :line
         line-bottom-start :line-start} (line/create line
                                                     origin-point point-bottom
                                                     :reversed? true
                                                     :real-start 0
                                                     :real-end arm-length
                                                     :context context
                                                     :environment environment)
        {line-left :line
         line-left-start :line-start} (line/create opposite-line
                                                   origin-point point-left
                                                   :flipped? true
                                                   :mirrored? true
                                                   :real-start 0
                                                   :real-end arm-length
                                                   :context context
                                                   :environment environment)
        parts [[["M" (v/+ point-top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [(v/+ point-left
                                      line-left-start)
                                 (v/+ point-top
                                      line-top-start)])
                 "z"]
                [top-left origin-point]]

               [["M" (v/+ point-top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/+ point-right
                                      line-right-start)
                                 (v/+ point-top
                                      line-top-start)])
                 "z"]
                [origin-point top-right]]

               [["M" (v/+ point-bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [(v/+ point-left
                                      line-left-start)
                                 (v/+ point-bottom
                                      line-bottom-start)])
                 "z"]
                [origin-point bottom-left]]

               [["M" (v/+ point-bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [(v/+ point-right
                                      line-right-start)
                                 (v/+ point-bottom
                                      line-bottom-start)])
                 "z"]
                [origin-point bottom-right]]]]
    [:<>
     [shared/make-subfields
      path parts
      [:all
       [(svg/make-path
         ["M" origin-point
          (svg/stitch line-right)])]
       [(svg/make-path
         ["M" (v/+ point-bottom
                   line-bottom-start)
          (svg/stitch line-bottom)])]
       nil]
      environment context]
     (when outline?
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ point-top
                              line-top-start)
                     (svg/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ point-bottom
                              line-bottom-start)
                     (svg/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-left)])}]])]))
