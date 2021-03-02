(ns heraldry.coat-of-arms.division.type.tierced-per-fess
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Tierced per fess"
   :value :tierced-per-fess
   :parts ["chief" "fess" "base"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout origin]} (options/sanitize division (division-options/options division))
        {:keys [stretch-y]} layout
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        height (:height environment)
        middle-half-height (-> height
                               (/ 6)
                               (* stretch-y))
        row1 (- (:y origin-point) middle-half-height)
        row2 (+ (:y origin-point) middle-half-height)
        first-left (v/v (:x left) row1)
        first-right (v/v (:x right) row1)
        second-left (v/v (:x left) row2)
        second-right (v/v (:x right) row2)
        {line-one :line
         line-one-start :line-start} (line/create line
                                                  (:x (v/- right left))
                                                  :render-options render-options)
        {line-reversed :line
         line-reversed-start :line-start} (line/create line
                                                       (:x (v/- right left))
                                                       :reversed? true
                                                       :flipped? true
                                                       :angle 180
                                                       :render-options render-options)
        parts [[["M" (v/+ first-left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/+ first-right
                                      line-one-start)
                                 (v/+ first-left
                                      line-one-start)])
                 "z"]
                [top-left (v/+ first-right
                               line-one-start)]]

               [["M" (v/+ first-left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/+ first-left
                                      line-one-start)
                                 (v/+ second-right
                                      line-reversed-start)])
                 (svg/stitch line-reversed)
                 (infinity/path :clockwise
                                [:left :left]
                                [(v/+ second-left
                                      line-reversed-start)
                                 (v/+ first-left
                                      line-one-start)])
                 "z"]
                [(v/+ first-left
                      line-one-start)
                 second-right]]

               [["M" (v/+ second-right
                          line-reversed-start)
                 (svg/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:left :right]
                                [(v/+ second-left
                                      line-reversed-start)
                                 (v/+ second-right
                                      line-reversed-start)])
                 "z"]
                [(v/+ second-left
                      line-reversed-start) bottom-right]]]]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all
       [(svg/make-path
         ["M" (v/+ second-right
                   line-reversed-start)
          (svg/stitch line-reversed)])]
       nil]
      environment division context]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ first-left
                              line-one-start)
                     (svg/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ second-right
                              line-reversed-start)
                     (svg/stitch line-reversed)])}]])]))
