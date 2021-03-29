(ns heraldry.coat-of-arms.division.type.per-pile
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.shared.pile :as pile]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Per pile"
   :value        :per-pile}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                geometry]}                 (options/sanitize division (division-options/options division))
        opposite-line                      (division-options/sanitize-opposite-line division line)
        anchor                             (-> anchor
                                               (assoc :type :edge))
        geometry                           (-> geometry
                                               (assoc :stretch 1))
        points                             (:points environment)
        top-left                           (:top-left points)
        top-right                          (:top-right points)
        bottom-left                        (:bottom-left points)
        bottom-right                       (:bottom-right points)
        thickness-base                     (if (#{:left :right} (:point origin))
                                             (:height environment)
                                             (:width environment))
        {origin-point :origin
         point        :point
         thickness    :thickness}          (pile/calculate-properties
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
         right-point :right}               (pile/diagonals origin-point point thickness)
        angle-left                         (angle/angle-to-point left-point point)
        angle-right                        (angle/angle-to-point right-point point)
        line                               (-> line
                                               (update-in [:fimbriation :thickness-1] (util/percent-of thickness-base))
                                               (update-in [:fimbriation :thickness-2] (util/percent-of thickness-base)))
        {line-left       :line
         line-left-start :line-start
         line-left-end   :line-end
         :as             line-left-data}   (line/create line
                                                        (v/abs (v/- left-point point))
                                                        :angle angle-left
                                                        :reversed? true
                                                        :render-options render-options)
        {line-right       :line
         line-right-start :line-start
         :as              line-right-data} (line/create opposite-line
                                                        (v/abs (v/- right-point point))
                                                        :angle (- angle-right 180)
                                                        :render-options render-options)
        parts                              [[["M" (v/+ left-point
                                                       line-left-end)
                                              (svg/stitch line-right)
                                              (infinity/path
                                               :counter-clockwise
                                               (cond
                                                 (#{:top-left
                                                    :top
                                                    :top-right} (:point origin))    [:top :bottom]
                                                 (#{:left} (:point origin))         [:left :right]
                                                 (#{:right} (:point origin))        [:right :left]
                                                 (#{:bottom-left
                                                    :bottom
                                                    :bottom-right} (:point origin)) [:bottom :top]
                                                 :else                              [:top :bottom])
                                               [(v/+ right-point
                                                     line-right-start)
                                                (v/+ left-point
                                                     line-left-end)])
                                              "z"]
                                             ;; TODO: these fields inherit the whole parent
                                             ;; environment points, but it can probably be reduced
                                             [top-left top-right
                                              bottom-left bottom-right]]

                                            [["M" (v/+ left-point
                                                       line-left-start)
                                              (svg/stitch line-left)
                                              (svg/stitch line-right)
                                              "z"]
                                             ;; TODO: these fields inherit the whole parent
                                             ;; environment points, but it can probably be reduced
                                             [top-left top-right
                                              bottom-left bottom-right]]

                                            [["M" (v/+ left-point
                                                       line-left-start)
                                              (svg/stitch line-left)
                                              (infinity/path
                                               :counter-clockwise
                                               (cond
                                                 (#{:top-left
                                                    :top
                                                    :top-right} (:point origin))    [:bottom :top]
                                                 (#{:left} (:point origin))         [:right :left]
                                                 (#{:right} (:point origin))        [:left :right]
                                                 (#{:bottom-left
                                                    :bottom
                                                    :bottom-right} (:point origin)) [:top :bottom]
                                                 :else                              [:bottom :top])
                                               [(v/+ left-point
                                                     line-left-end)
                                                (v/+ left-point
                                                     line-left-start)])
                                              "z"]
                                             ;; TODO: these fields inherit the whole parent
                                             ;; environment points, but it can probably be reduced
                                             [top-left top-right
                                              bottom-left bottom-right]]]

        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil nil]
      environment division context]
     (line/render line [line-left-data
                        line-right-data] left-point outline? render-options)]))

