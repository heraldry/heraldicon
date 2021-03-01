(ns heraldry.coat-of-arms.ordinary.type.saltire
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
  {:display-name "Saltire"
   :value        :saltire}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}                 (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                                               geometry
        points                                                       (:points environment)
        origin-point                                                 (position/calculate origin environment :fess)
        top                                                          (assoc (:top points) :x (:x origin-point))
        bottom                                                       (assoc (:bottom points) :x (:x origin-point))
        left                                                         (assoc (:left points) :y (:y origin-point))
        right                                                        (assoc (:right points) :y (:y origin-point))
        width                                                        (:width environment)
        height                                                       (:height environment)
        band-width                                                   (-> size
                                                                         ((util/percent-of width)))
        direction                                                    (angle/direction diagonal-mode points origin-point)
        arm-length                                                   (-> direction
                                                                         v/abs
                                                                         (->> (/ height)))
        diagonal-top-left                                            (v/+ origin-point (v/* (v/dot direction (v/v -1 -1)) arm-length))
        diagonal-top-right                                           (v/+ origin-point (v/* (v/dot direction (v/v 1 -1)) arm-length))
        diagonal-bottom-left                                         (v/+ origin-point (v/* (v/dot direction (v/v -1 1)) arm-length))
        diagonal-bottom-right                                        (v/+ origin-point (v/* (v/dot direction (v/v 1 1)) arm-length))
        angle-top-left                                               (angle/angle-to-point origin-point diagonal-top-left)
        angle-top-right                                              (angle/angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                                            (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                                           (angle/angle-to-point origin-point diagonal-bottom-right)
        angle                                                        (-> angle-bottom-right (* Math/PI) (/ 180))
        joint-angle-horizontal                                       (+ 360 (- angle-top-left angle-bottom-left))
        joint-angle-vertical                                         (- angle-top-right angle-top-left)
        dx                                                           (/ band-width 2 (Math/sin angle))
        dy                                                           (/ band-width 2 (Math/cos angle))
        offset-top                                                   (v/v 0 (- dy))
        offset-bottom                                                (v/v 0 dy)
        offset-left                                                  (v/v (- dx) 0)
        offset-right                                                 (v/v dx 0)
        corner-top                                                   (v/+ origin-point offset-top)
        corner-bottom                                                (v/+ origin-point offset-bottom)
        corner-left                                                  (v/+ origin-point offset-left)
        corner-right                                                 (v/+ origin-point offset-right)
        top-left-upper                                               (v/+ diagonal-top-left offset-top)
        top-left-lower                                               (v/+ diagonal-top-left offset-bottom)
        top-right-upper                                              (v/+ diagonal-top-right offset-top)
        top-right-lower                                              (v/+ diagonal-top-right offset-bottom)
        bottom-left-upper                                            (v/+ diagonal-bottom-left offset-top)
        bottom-left-lower                                            (v/+ diagonal-bottom-left offset-bottom)
        bottom-right-upper                                           (v/+ diagonal-bottom-right offset-top)
        bottom-right-lower                                           (v/+ diagonal-bottom-right offset-bottom)
        line                                                         (-> line
                                                                         (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                                         (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-top-left-lower       :line
         line-top-left-lower-start :line-start
         :as                       line-top-left-lower-data}         (line/create line
                                                                                  (v/abs (v/- corner-left top-left-lower))
                                                                                  :angle  angle-top-left
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options   render-options)
        {line-top-left-upper       :line
         line-top-left-upper-start :line-start
         :as                       line-top-left-upper-data}         (line/create line
                                                                                  (v/abs (v/- corner-top top-left-upper))
                                                                                  :angle     (- angle-top-left 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-top-right-upper       :line
         line-top-right-upper-start :line-start
         :as                        line-top-right-upper-data}       (line/create line
                                                                                  (v/abs (v/- corner-top top-right-upper))
                                                                                  :angle angle-top-right
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-top-right-lower       :line
         line-top-right-lower-start :line-start
         :as                        line-top-right-lower-data}       (line/create line
                                                                                  (v/abs (v/- corner-right top-right-lower))
                                                                                  :angle (- angle-top-right 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options render-options)
        {line-bottom-right-upper       :line
         line-bottom-right-upper-start :line-start
         :as                           line-bottom-right-upper-data} (line/create line
                                                                                  (v/abs (v/- corner-right bottom-right-upper))
                                                                                  :angle angle-bottom-right
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options render-options)
        {line-bottom-right-lower       :line
         line-bottom-right-lower-start :line-start
         :as                           line-bottom-right-lower-data} (line/create line
                                                                                  (v/abs (v/- corner-bottom bottom-right-lower))
                                                                                  :angle (- angle-bottom-right 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-bottom-left-lower       :line
         line-bottom-left-lower-start :line-start
         :as                          line-bottom-left-lower-data}   (line/create line
                                                                                  (v/abs (v/- corner-bottom bottom-left-lower))
                                                                                  :angle angle-bottom-left
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-bottom-left-upper       :line
         line-bottom-left-upper-start :line-start
         :as                          line-bottom-left-upper-data}   (line/create line
                                                                                  (v/abs (v/- corner-left bottom-left-upper))
                                                                                  :angle (- angle-bottom-left 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options render-options)
        parts                                                        [[["M" (v/+ corner-left
                                                                                 line-top-left-lower-start)
                                                                        (svg/stitch line-top-left-lower)
                                                                        (infinity/path :clockwise
                                                                                       [:left :left]
                                                                                       [(v/+ top-left-lower
                                                                                             line-top-left-lower-start)
                                                                                        (v/+ top-left-upper
                                                                                             line-top-left-upper-start)])
                                                                        (svg/stitch line-top-left-upper)
                                                                        "L" (v/+ corner-top
                                                                                 line-top-right-upper-start)
                                                                        (svg/stitch line-top-right-upper)
                                                                        (infinity/path :clockwise
                                                                                       [:right :right]
                                                                                       [(v/+ top-right-upper
                                                                                             line-top-right-upper-start)
                                                                                        (v/+ top-right-lower
                                                                                             line-top-right-lower-start)])
                                                                        (svg/stitch line-top-right-lower)
                                                                        "L" (v/+ corner-right
                                                                                 line-bottom-right-upper-start)
                                                                        (svg/stitch line-bottom-right-upper)
                                                                        (infinity/path :clockwise
                                                                                       [:right :right]
                                                                                       [(v/+ bottom-right-upper
                                                                                             line-bottom-right-upper-start)
                                                                                        (v/+ bottom-right-lower
                                                                                             line-bottom-right-lower-start)])
                                                                        (svg/stitch line-bottom-right-lower)
                                                                        "L" (v/+ corner-bottom
                                                                                 line-bottom-left-lower-start)
                                                                        (svg/stitch line-bottom-left-lower)
                                                                        (infinity/path :clockwise
                                                                                       [:left :left]
                                                                                       [(v/+ bottom-left-lower
                                                                                             line-bottom-left-lower)
                                                                                        (v/+ bottom-left-upper
                                                                                             line-bottom-left-upper-start)])
                                                                        (svg/stitch line-bottom-left-upper)
                                                                        "z"]
                                                                       [top bottom left right]]]
        field                                                        (if (counterchange/counterchangable? field parent)
                                                                       (counterchange/counterchange-field field parent)
                                                                       field)
        [fimbriation-elements-1 fimbriation-outlines-1]              (fimbriation/render
                                                                      [top-left-upper :left]
                                                                      [top-right-upper :right]
                                                                      [line-top-left-upper-data
                                                                       line-top-right-upper-data]
                                                                      (:fimbriation line)
                                                                      render-options)
        [fimbriation-elements-2 fimbriation-outlines-2]              (fimbriation/render
                                                                      [top-right-lower :right]
                                                                      [bottom-right-upper :right]
                                                                      [line-top-right-lower-data
                                                                       line-bottom-right-upper-data]
                                                                      (:fimbriation line)
                                                                      render-options)
        [fimbriation-elements-3 fimbriation-outlines-3]              (fimbriation/render
                                                                      [bottom-right-lower :right]
                                                                      [bottom-left-lower :left]
                                                                      [line-bottom-right-lower-data
                                                                       line-bottom-left-lower-data]
                                                                      (:fimbriation line)
                                                                      render-options)
        [fimbriation-elements-4 fimbriation-outlines-4]              (fimbriation/render
                                                                      [bottom-left-upper :left]
                                                                      [top-left-lower :left]
                                                                      [line-bottom-left-upper-data
                                                                       line-top-left-lower-data]
                                                                      (:fimbriation line)
                                                                      render-options)]

    [:<>
     fimbriation-elements-1
     fimbriation-elements-2
     fimbriation-elements-3
     fimbriation-elements-4
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-left
                               line-top-left-lower-start)
                      (svg/stitch line-top-left-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ top-left-upper
                               line-top-left-upper-start)
                      (svg/stitch line-top-left-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top
                               line-top-right-upper-start)
                      (svg/stitch line-top-right-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ top-right-lower
                               line-top-right-lower-start)
                      (svg/stitch line-top-right-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-right
                               line-bottom-right-upper-start)
                      (svg/stitch line-bottom-right-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ bottom-right-lower
                               line-bottom-right-lower-start)
                      (svg/stitch line-bottom-right-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom
                               line-bottom-left-lower-start)
                      (svg/stitch line-bottom-left-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ bottom-left-upper
                               line-bottom-left-upper-start)
                      (svg/stitch line-bottom-left-upper)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2
         fimbriation-outlines-3
         fimbriation-outlines-4])
      environment ordinary context]]))
