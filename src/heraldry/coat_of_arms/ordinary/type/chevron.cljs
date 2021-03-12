(ns heraldry.coat-of-arms.ordinary.type.chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Chevron"
   :value        :chevron}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}   (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                                 geometry
        opposite-line                                  (ordinary-options/sanitize-opposite-line ordinary line)
        points                                         (:points environment)
        origin-point                                   (position/calculate origin environment :fess)
        top                                            (assoc (:top points) :x (:x origin-point))
        bottom                                         (assoc (:bottom points) :x (:x origin-point))
        left                                           (assoc (:left points) :y (:y origin-point))
        right                                          (assoc (:right points) :y (:y origin-point))
        height                                         (:height environment)
        band-width                                     (-> size
                                                           ((util/percent-of height)))
        direction                                      (angle/direction diagonal-mode points origin-point)
        diagonal-left                                  (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-right                                 (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-left                                     (angle/angle-to-point origin-point diagonal-left)
        angle-right                                    (angle/angle-to-point origin-point diagonal-right)
        angle                                          (-> angle-right (* Math/PI) (/ 180))
        joint-angle                                    (- angle-left angle-right)
        dy                                             (/ band-width 2 (Math/cos angle))
        offset-top                                     (v/v 0 (- dy))
        offset-bottom                                  (v/v 0 dy)
        corner-upper                                   (v/+ origin-point offset-top)
        corner-lower                                   (v/+ origin-point offset-bottom)
        left-upper                                     (v/+ diagonal-left offset-top)
        left-lower                                     (v/+ diagonal-left offset-bottom)
        right-upper                                    (v/+ diagonal-right offset-top)
        right-lower                                    (v/+ diagonal-right offset-bottom)
        line                                           (-> line
                                                           (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                           (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line                                  (-> opposite-line
                                                           (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                           (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-right-upper       :line
         line-right-upper-start :line-start
         :as                    line-right-upper-data} (line/create line
                                                                    (v/abs (v/- corner-upper right-upper))
                                                                    :angle angle-right
                                                                    :render-options render-options)
        {line-right-lower       :line
         line-right-lower-start :line-start
         :as                    line-right-lower-data} (line/create opposite-line
                                                                    (v/abs (v/- corner-lower right-lower))
                                                                    :angle (- angle-right 180)
                                                                    :reversed? true
                                                                    :render-options render-options)
        {line-left-lower       :line
         line-left-lower-start :line-start
         :as                   line-left-lower-data}   (line/create opposite-line
                                                                    (v/abs (v/- corner-lower left-lower))
                                                                    :angle angle-left
                                                                    :render-options render-options)
        {line-left-upper       :line
         line-left-upper-start :line-start
         :as                   line-left-upper-data}   (line/create line
                                                                    (v/abs (v/- corner-upper left-upper))
                                                                    :angle (- angle-left 180)
                                                                    :reversed? true
                                                                    :render-options render-options)
        parts                                          [[["M" (v/+ corner-upper
                                                                   line-right-upper-start)
                                                          (svg/stitch line-right-upper)
                                                          (infinity/path :clockwise
                                                                         [:right :right]
                                                                         [(v/+ right-upper
                                                                               line-right-upper-start)
                                                                          (v/+ right-lower
                                                                               line-right-lower-start)])
                                                          (svg/stitch line-right-lower)
                                                          "L" (v/+ corner-lower
                                                                   line-left-lower-start)
                                                          (svg/stitch line-left-lower)
                                                          (infinity/path :clockwise
                                                                         [:left :left]
                                                                         [(v/+ left-lower
                                                                               line-left-lower-start)
                                                                          (v/+ left-upper
                                                                               line-left-upper-start)])
                                                          (svg/stitch line-left-upper)
                                                          "z"]
                                                         [top bottom left right]]]
        field                                          (if (counterchange/counterchangable? field parent)
                                                         (counterchange/counterchange-field field parent)
                                                         field)
        outline?                                       (or (:outline? render-options)
                                                           (:outline? hints))]
    [:<>
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-left-upper-data
                        line-right-upper-data] left-upper outline? render-options)
     (line/render opposite-line [line-right-lower-data
                                 line-left-lower-data] right-lower outline? render-options)]))
