(ns heraldry.coat-of-arms.ordinary.type.bend-sinister
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Bend sinister"
   :value :bend-sinister}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]} (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]} geometry
        opposite-line (ordinary-options/sanitize-opposite-line ordinary line)
        points (:points environment)
        top-right (:top-right points)
        origin-point (position/calculate origin environment :fess)
        left (:left points)
        right (:right points)
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        direction (angle/direction diagonal-mode points origin-point)
        direction-orthogonal (v/v (-> direction :y) (-> direction :x))
        diagonal-start (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-end (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle (angle/angle-to-point diagonal-start diagonal-end)
        required-half-length (v/distance-point-to-line top-right origin-point (v/+ origin-point direction-orthogonal))
        bend-length (* required-half-length 2)
        line-length (-> diagonal-end
                        (v/- diagonal-start)
                        v/abs
                        (* 4))
        row1 (- (/ band-height 2))
        row2 (+ row1 band-height)
        period (-> line
                   :width
                   (or 1))
        offset (-> period
                   (* (-> line-length
                          (/ 4)
                          (/ period)
                          Math/ceil
                          inc))
                   -)
        first-left (v/v offset row1)
        first-right (v/v (+ offset line-length) row1)
        second-left (v/v offset row2)
        second-right (v/v (+ offset line-length) row2)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (util/percent-of height))
                          (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one :line
         line-one-start :line-start
         :as line-one-data} (line/create line
                                         line-length
                                         :render-options render-options)
        {line-reversed :line
         line-reversed-start :line-start
         :as line-reversed-data} (line/create opposite-line
                                              line-length
                                              :reversed? true
                                              :angle 180
                                              :render-options render-options)
        parts [[["M" (v/+ first-left
                          line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/+ first-right
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
                [(v/v 0 row1) (v/v bend-length row2)]]]
        counterchanged? (counterchange/counterchangable? field parent)
        field (if counterchanged?
                (counterchange/counterchange-field field parent)
                field)
        [fimbriation-elements-top fimbriation-outlines-top] (fimbriation/render
                                                             [first-left :left]
                                                             [first-right :right]
                                                             [line-one-data]
                                                             (:fimbriation line)
                                                             render-options)
        [fimbriation-elements-bottom fimbriation-outlines-bottom] (fimbriation/render
                                                                   [second-right :right]
                                                                   [second-left :left]
                                                                   [line-reversed-data]
                                                                   (:fimbriation opposite-line)
                                                                   render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")"
                         "translate(" (- required-half-length) "," 0 ")")}
     fimbriation-elements-top
     fimbriation-elements-bottom
     [division-shared/make-division
      :ordinary-fess [field] parts
      [:all]
      environment
      ordinary
      (-> context
          (assoc :transform (when (or counterchanged?
                                      (:inherit-environment? field))
                              (str
                               "rotate(" (- angle) ") "
                               "translate(" (-> diagonal-start :x -) "," (-> diagonal-start :y -) ")"))))]
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
                     (svg/stitch line-reversed)])}]
        fimbriation-outlines-top
        fimbriation-outlines-bottom])]))
