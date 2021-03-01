(ns heraldry.coat-of-arms.division.type.quartered
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
  {:display-name "Quarterly 2x2"
   :value :quartered
   :parts ["I" "II" "III" "IV"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]} (options/sanitize division (division-options/options division))
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
                                                    :flipped? true
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
                                                   :angle -180
                                                   :flipped? true
                                                   :render-options render-options)
        parts [[["M" (v/+ top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [(v/+ left
                                      line-left-start)
                                 (v/+ top
                                      line-top-start)])
                 "z"]
                [top-left origin-point]]

               [["M" (v/+ top
                          line-top-start)
                 (svg/stitch line-top)
                 "L" origin-point
                 (svg/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/+ right
                                      line-right-start)
                                 (v/+ top
                                      line-top-start)])
                 "z"]
                [origin-point top-right]]

               [["M" (v/+ bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [(v/+ left
                                      line-left-start)
                                 (v/+ bottom
                                      line-bottom-start)])
                 "z"]
                [origin-point bottom-left]]

               [["M" (v/+ bottom
                          line-bottom-start)
                 (svg/stitch line-bottom)
                 "L" origin-point
                 (svg/stitch line-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [(v/+ right
                                      line-right-start)
                                 (v/+ bottom
                                      line-bottom-start)])
                 "z"]
                [origin-point bottom-right]]]]
    [shared/make-division
     (shared/division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (svg/stitch line-right)])]
      [(svg/make-path
        ["M" (v/+ bottom
                  line-bottom-start)
         (svg/stitch line-bottom)])]
      nil]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ top
                              line-top-start)
                     (svg/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ bottom
                              line-bottom-start)
                     (svg/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (svg/stitch line-left)])}]])
     environment division context]))
