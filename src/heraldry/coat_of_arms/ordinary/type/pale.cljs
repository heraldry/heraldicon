(ns heraldry.coat-of-arms.ordinary.type.pale
  (:require [heraldry.coat-of-arms.charge.core :as charge]
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
  {:display-name "Pale"
   :value        :pale}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}                          (options/sanitize ordinary (ordinary-options/options ordinary))
        opposite-line                                           (ordinary-options/sanitize-opposite-line ordinary line)
        {:keys [size]}                                          geometry
        points                                                  (:points environment)
        origin-point                                            (position/calculate origin environment :fess)
        top                                                     (assoc (:top points) :x (:x origin-point))
        bottom                                                  (assoc (:bottom points) :x (:x origin-point))
        width                                                   (:width environment)
        band-width                                              (-> size
                                                                    ((util/percent-of width)))
        col1                                                    (- (:x origin-point) (/ band-width 2))
        col2                                                    (+ col1 band-width)
        first-top                                               (v/v col1 (:y top))
        first-bottom                                            (v/v col1 (:y bottom))
        second-top                                              (v/v col2 (:y top))
        second-bottom                                           (v/v col2 (:y bottom))
        line                                                    (-> line
                                                                    (update-in [:fimbriation :thickness-1] (util/percent-of width))
                                                                    (update-in [:fimbriation :thickness-2] (util/percent-of width)))
        opposite-line                                           (-> opposite-line
                                                                    (update-in [:fimbriation :thickness-1] (util/percent-of width))
                                                                    (update-in [:fimbriation :thickness-2] (util/percent-of width)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}                          (line/create line
                                                                             (:y (v/- bottom top))
                                                                             :angle -90
                                                                             :reversed? true
                                                                             :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data}                (line/create opposite-line
                                                                             (:y (v/- bottom top))
                                                                             :angle 90
                                                                             :render-options render-options)
        parts                                                   [[["M" (v/+ first-bottom
                                                                            line-one-start)
                                                                   (svg/stitch line-one)
                                                                   (infinity/path :clockwise
                                                                                  [:top :top]
                                                                                  [(v/+ first-top
                                                                                        line-one-start)
                                                                                   (v/+ second-top
                                                                                        line-reversed-start)])
                                                                   (svg/stitch line-reversed)
                                                                   (infinity/path :clockwise
                                                                                  [:bottom :bottom]
                                                                                  [(v/+ second-bottom
                                                                                        line-reversed-start)
                                                                                   (v/+ first-bottom
                                                                                        line-one-start)])
                                                                   "z"]
                                                                  [(v/+ first-bottom
                                                                        line-one-start)
                                                                   (v/+ second-top
                                                                        line-reversed-start)]]]
        field                                                   (if (charge/counterchangable? field parent)
                                                                  (charge/counterchange-field field parent)
                                                                  field)
        [fimbriation-elements-left fimbriation-outlines-left]   (fimbriation/render
                                                                 [first-bottom :bottom]
                                                                 [first-top :top]
                                                                 [line-one-data]
                                                                 (:fimbriation line)
                                                                 render-options)
        [fimbriation-elements-right fimbriation-outlines-right] (fimbriation/render
                                                                 [second-top :top]
                                                                 [second-bottom :bottom]
                                                                 [line-reversed-data]
                                                                 (:fimbriation opposite-line)
                                                                 render-options)]
    [:<>
     fimbriation-elements-left
     fimbriation-elements-right
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-bottom
                               line-one-start)
                      (svg/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-top
                               line-reversed-start)
                      (svg/stitch line-reversed)])}]
         fimbriation-outlines-left
         fimbriation-outlines-right])
      environment ordinary context]]))
