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
        intersection-top-left (v/find-first-intersection-of-ray origin-point diagonal-top-left environment)
        intersection-top-right (v/find-first-intersection-of-ray origin-point diagonal-top-right environment)
        intersection-bottom-left (v/find-first-intersection-of-ray origin-point diagonal-bottom-left environment)
        intersection-bottom-right (v/find-first-intersection-of-ray origin-point diagonal-bottom-right environment)
        intersection-top (v/find-first-intersection-of-ray origin-point top environment)
        intersection-bottom (v/find-first-intersection-of-ray origin-point bottom environment)
        intersection-left (v/find-first-intersection-of-ray origin-point left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point right environment)
        arm-length (->> [intersection-top-left
                         intersection-top-right
                         intersection-bottom-left
                         intersection-bottom-right
                         intersection-top
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
        point-top-left diagonal-top-left
        point-top-right diagonal-top-right
        point-bottom-left diagonal-bottom-left
        point-bottom-right diagonal-bottom-right
        line (-> line
                 (dissoc :fimbriation))
        {line-top :line
         line-top-start :line-start} (line/create2 line
                                                   origin-point point-top
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end arm-length
                                                   :render-options render-options
                                                   :environment environment)
        {line-right :line
         line-right-start :line-start} (line/create2 line
                                                     origin-point point-right
                                                     :reversed? true
                                                     :real-start 0
                                                     :real-end arm-length
                                                     :render-options render-options
                                                     :environment environment)
        {line-bottom :line
         line-bottom-start :line-start} (line/create2 line
                                                      origin-point point-bottom
                                                      :reversed? true
                                                      :real-start 0
                                                      :real-end arm-length
                                                      :render-options render-options
                                                      :environment environment)
        {line-left :line
         line-left-start :line-start} (line/create2 line
                                                    origin-point point-left
                                                    :reversed? true
                                                    :real-start 0
                                                    :real-end arm-length
                                                    :render-options render-options
                                                    :environment environment)
        {line-top-left :line} (line/create2 line
                                            origin-point point-top-left
                                            :flipped? true
                                            :real-start 0
                                            :real-end arm-length
                                            :render-options render-options
                                            :environment environment)
        {line-top-right :line} (line/create2 line
                                             origin-point point-top-right
                                             :flipped? true
                                             :real-start 0
                                             :real-end arm-length
                                             :render-options render-options
                                             :environment environment)
        {line-bottom-right :line} (line/create2 line
                                                origin-point point-bottom-right
                                                :flipped? true
                                                :real-start 0
                                                :real-end arm-length
                                                :render-options render-options
                                                :environment environment)
        {line-bottom-left :line} (line/create2 line
                                               origin-point point-bottom-left
                                               :flipped? true
                                               :real-start 0
                                               :real-end arm-length
                                               :render-options render-options
                                               :environment environment)
        parts [[["M" (v/+ point-top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-top-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [point-top-left
                                 (v/+ point-top
                                      line-top-start)])
                 "z"]
                [top-left
                 origin-point
                 top]]

               [["M" (v/+ point-top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [point-top-right
                                 (v/+ point-top
                                      line-top-start)])
                 "z"]
                [top
                 origin-point
                 top-right]]

               [["M" (v/+ point-left
                          line-left-start)
                 (svg/stitch line-left)
                 "L" origin-point
                 (svg/stitch line-top-left)
                 (infinity/path :counter-clockwise
                                [:left :left]
                                [point-top-left
                                 (v/+ point-left
                                      line-left-start)])
                 "z"]
                [left
                 origin-point
                 top-left]]

               [["M" (v/+ point-right
                          line-right-start)
                 (svg/stitch line-right)
                 "L" origin-point
                 (svg/stitch line-top-right)
                 (infinity/path :clockwise
                                [:right :right]
                                [point-top-right
                                 (v/+ point-right
                                      line-right-start)])
                 "z"]
                [top-right
                 origin-point
                 right]]

               [["M" (v/+ point-left
                          line-left-start)
                 (svg/stitch line-left)
                 "L" origin-point
                 (svg/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:left :left]
                                [point-bottom-left
                                 (v/+ point-left
                                      line-left-start)])
                 "z"]
                [bottom-left
                 origin-point
                 left]]

               [["M" (v/+ point-right
                          line-right-start)
                 (svg/stitch line-right)
                 "L" origin-point
                 (svg/stitch line-bottom-right)
                 (infinity/path :counter-clockwise
                                [:right :right]
                                [point-bottom-right
                                 (v/+ point-right
                                      line-right-start)])
                 "z"]
                [right
                 origin-point
                 bottom-right]]

               [["M" (v/+ point-bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [point-bottom-left
                                 (v/+ point-bottom
                                      line-bottom-start)])
                 "z"]
                [bottom
                 origin-point
                 bottom-left]]

               [["M" (v/+ point-bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-bottom-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [point-bottom-right
                                 (v/+ point-bottom
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
         ["M" (v/+ point-left
                   line-left-start)
          (svg/stitch line-left)])]
       [(svg/make-path
         ["M" (v/+ point-right
                   line-right-start)
          (svg/stitch line-right)])]
       [(svg/make-path
         ["M" origin-point
          (svg/stitch line-bottom-left)])]
       [(svg/make-path
         ["M" origin-point
          (svg/stitch line-bottom-right)])]
       [(svg/make-path
         ["M" (v/+ point-bottom
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
                    ["M" (v/+ point-top
                              line-top-start)
                     (svg/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ point-right
                              line-right-start)
                     (svg/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ point-bottom
                              line-bottom-start)
                     (svg/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ point-left
                              line-left-start)
                     (svg/stitch line-left)])}]])]))
