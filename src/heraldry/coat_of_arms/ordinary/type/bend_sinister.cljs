(ns heraldry.coat-of-arms.ordinary.type.bend-sinister
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Bend sinister"
   :value        :bend-sinister}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor geometry]}    (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                           geometry
        opposite-line                            (ordinary-options/sanitize-opposite-line ordinary line)
        points                                   (:points environment)
        top-right                                (:top-right points)
        left                                     (:left points)
        right                                    (:right points)
        top                                      (:top points)
        bottom                                   (:bottom points)
        height                                   (:height environment)
        band-height                              (-> size
                                                     ((util/percent-of height)))
        {origin-point :real-origin
         anchor-point :real-anchor}              (angle/calculate-origin-and-anchor
                                                  environment
                                                  origin
                                                  (update anchor :angle #(when %
                                                                           (- %)))
                                                  band-height
                                                  nil)
        center-point                             (v/line-intersection origin-point anchor-point
                                                                      top bottom)
        direction                                (v/- anchor-point origin-point)
        direction                                (v/v (-> direction :x Math/abs)
                                                      (-> direction :y Math/abs))
        direction-orthogonal                     (v/v (-> direction :y) (-> direction :x))
        diagonal-start                           (v/project-x center-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-end                             (v/project-x center-point (v/dot direction (v/v 1 -1)) (:x right))
        angle                                    (angle/angle-to-point diagonal-start diagonal-end)
        required-half-length                     (v/distance-point-to-line top-right center-point (v/+ center-point direction-orthogonal))
        bend-length                              (* required-half-length 2)
        line-length                              (-> diagonal-end
                                                     (v/- diagonal-start)
                                                     v/abs
                                                     (* 4))
        row1                                     (- (/ band-height 2))
        row2                                     (+ row1 band-height)
        period                                   (-> line
                                                     :width
                                                     (or 1))
        offset                                   (-> period
                                                     (* (-> line-length
                                                            (/ 4)
                                                            (/ period)
                                                            Math/ceil
                                                            inc))
                                                     -)
        first-left                               (v/v offset row1)
        second-right                             (v/v (+ offset line-length) row2)
        line                                     (-> line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line                            (-> opposite-line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}           (line/create line
                                                              line-length
                                                              :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data} (line/create opposite-line
                                                              line-length
                                                              :reversed? true
                                                              :angle 180
                                                              :render-options render-options)
        parts                                    [[["M" (v/+ first-left
                                                             line-one-start)
                                                    (svg/stitch line-one)
                                                    "L" (v/+ second-right
                                                             line-reversed-start)
                                                    (svg/stitch line-reversed)
                                                    "L" (v/+ first-left
                                                             line-one-start)
                                                    "z"]
                                                   [(v/v 0 row1) (v/v bend-length row2)]]]
        counterchanged?                          (counterchange/counterchangable? field parent)
        field                                    (if counterchanged?
                                                   (counterchange/counterchange-field field parent)
                                                   field)
        outline?                                 (or (:outline? render-options)
                                                     (:outline? hints))]
    [:g {:transform (str "translate(" (:x center-point) "," (:y center-point) ")"
                         "rotate(" angle ")"
                         "translate(" (- required-half-length) "," 0 ")")}
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
     (line/render line [line-one-data] first-left outline? render-options)
     (line/render opposite-line [line-reversed-data] second-right outline? render-options)]))
