(ns heraldry.coat-of-arms.field.type.per-pile
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.shared.pile :as pile]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Per pile"
   :value :per-pile}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                geometry]} (options/sanitize division (field-options/options division))
        opposite-line (field-options/sanitize-opposite-line division line)
        anchor (-> anchor
                   (assoc :type :edge))
        geometry (-> geometry
                     (assoc :stretch 1))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        thickness-base (if (#{:left :right} (:point origin))
                         (:height environment)
                         (:width environment))
        {origin-point :origin
         point :point
         thickness :thickness} (pile/calculate-properties
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
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        {left-point :left
         right-point :right} (pile/diagonals origin-point point thickness)
        intersection-left (-> (v/environment-intersections point left-point environment)
                              last)
        intersection-right (-> (v/environment-intersections point right-point environment)
                               last)
        end-left (-> intersection-left
                     (v/- point)
                     v/abs)
        end-right (-> intersection-right
                      (v/- point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of thickness-base))
                 (update-in [:fimbriation :thickness-2] (util/percent-of thickness-base)))
        {line-left :line
         line-left-start :line-start
         line-left-end :line-end
         :as line-left-data} (line/create line
                                          point left-point
                                          :reversed? true
                                          :real-start 0
                                          :real-end end
                                          :render-options render-options
                                          :environment environment)
        {line-right :line
         line-right-start :line-start
         line-right-end :line-end
         :as line-right-data} (line/create opposite-line
                                           point right-point
                                           :real-start 0
                                           :real-end end
                                           :render-options render-options
                                           :environment environment)
        parts [[["M" (v/+ point
                          line-right-start)
                 (svg/stitch line-right)
                 (infinity/path
                  :counter-clockwise
                  (cond
                    (#{:top-left
                       :top
                       :top-right} (:point origin)) [:top :bottom]
                    (#{:left} (:point origin)) [:left :right]
                    (#{:right} (:point origin)) [:right :left]
                    (#{:bottom-left
                       :bottom
                       :bottom-right} (:point origin)) [:bottom :top]
                    :else [:top :bottom])
                  [(v/+ point
                        line-right-end)
                   (v/+ point
                        line-right-start)])
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
                       :top-right} (:point origin)) [:bottom :top]
                    (#{:left} (:point origin)) [:right :left]
                    (#{:right} (:point origin)) [:left :right]
                    (#{:bottom-left
                       :bottom
                       :bottom-right} (:point origin)) [:top :bottom]
                    :else [:bottom :top])
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
     [shared/make-subfields
      (shared/field-context-key type) fields parts
      [:all nil nil]
      environment division context]
     (line/render line [line-left-data
                        line-right-data] left-point outline? render-options)]))
