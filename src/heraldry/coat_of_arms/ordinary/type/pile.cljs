(ns heraldry.coat-of-arms.ordinary.type.pile
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.shared.pile :as pile]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Pile"
   :value        :pile}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                geometry]}               (options/sanitize ordinary (ordinary-options/options ordinary))
        points                           (:points environment)
        top-left                         (:top-left points)
        top-right                        (:top-right points)
        bottom-left                      (:bottom-left points)
        bottom-right                     (:bottom-right points)
        thickness-base                   (if (#{:left :right} (:point origin))
                                           (:height environment)
                                           (:width environment))
        {origin-point :origin
         point        :point
         thickness    :thickness}        (pile/calculate-properties
                                          environment
                                          origin
                                          (cond-> anchor
                                            (#{:top-right
                                               :right
                                               :bottom-left} (:point origin)) (update :angle #(when %
                                                                                                (- %))))
                                          geometry
                                          thickness-base
                                          (case (:point origin)
                                            :top-left     0
                                            :top          90
                                            :top-right    180
                                            :left         0
                                            :right        180
                                            :bottom-left  0
                                            :bottom       -90
                                            :bottom-right 180
                                            0))
        {left-point  :left
         right-point :right}             (pile/diagonals origin-point point thickness)
        angle-left                       (angle/angle-to-point left-point point)
        angle-right                      (angle/angle-to-point right-point point)
        line                             (-> line
                                             (update-in [:fimbriation :thickness-1] (util/percent-of thickness-base))
                                             (update-in [:fimbriation :thickness-2] (util/percent-of thickness-base)))
        {line-left       :line
         line-left-start :line-start
         :as             line-left-data} (line/create line
                                                      (v/abs (v/- left-point point))
                                                      :angle angle-left
                                                      :reversed? true
                                                      :render-options render-options)
        {line-right :line
         :as        line-right-data}     (line/create line
                                                      (v/abs (v/- right-point point))
                                                      :angle (- angle-right 180)
                                                      :render-options render-options)
        parts                            [[["M" (v/+ left-point
                                                     line-left-start)
                                            (svg/stitch line-left)
                                            (svg/stitch line-right)
                                            "z"]
                                           [top-left top-right
                                            bottom-left bottom-right]]]
        field                            (if (counterchange/counterchangable? field parent)
                                           (counterchange/counterchange-field field parent)
                                           field)
        outline?                         (or (:outline? render-options)
                                             (:outline? hints))]
    [:<>
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-left-data
                        line-right-data] left-point outline? render-options)]))
