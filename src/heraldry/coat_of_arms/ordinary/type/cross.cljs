(ns heraldry.coat-of-arms.ordinary.type.cross
  (:require [heraldry.coat-of-arms.charge :as charge]
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
  {:display-name "Cross"
   :value :cross}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]} (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]} geometry
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x origin-point))
        bottom (assoc (:bottom points) :x (:x origin-point))
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of width)))
        col1 (- (:x origin-point) (/ band-width 2))
        col2 (+ col1 band-width)
        pale-top-left (v/v col1 (:y top))
        pale-bottom-left (v/v col1 (:y bottom))
        pale-top-right (v/v col2 (:y top))
        pale-bottom-right (v/v col2 (:y bottom))
        row1 (- (:y origin-point) (/ band-width 2))
        row2 (+ row1 band-width)
        fess-top-left (v/v (:x left) row1)
        fess-top-right (v/v (:x right) row1)
        fess-bottom-left (v/v (:x left) row2)
        fess-bottom-right (v/v (:x right) row2)
        corner-top-left (v/v col1 row1)
        corner-top-right (v/v col2 row1)
        corner-bottom-left (v/v col1 row2)
        corner-bottom-right (v/v col2 row2)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-pale-top-left :line
         line-pale-top-left-start :line-start
         :as line-pale-top-left-data} (line/create line
                                                   (v/abs (v/- corner-top-left pale-top-left))
                                                   :angle -90
                                                   :joint-angle 90
                                                   :render-options render-options)
        {line-pale-top-right :line
         line-pale-top-right-start :line-start
         :as line-pale-top-right-data} (line/create line
                                                    (v/abs (v/- corner-top-right pale-top-right))
                                                    :angle 90
                                                    :reversed? true
                                                    :joint-angle 90
                                                    :render-options render-options)
        {line-fess-top-right :line
         line-fess-top-right-start :line-start
         :as line-fess-top-right-data} (line/create line
                                                    (v/abs (v/- corner-top-right fess-top-right))
                                                    :joint-angle 90
                                                    :render-options render-options)
        {line-fess-bottom-right :line
         line-fess-bottom-right-start :line-start
         :as line-fess-bottom-right-data} (line/create line
                                                       (v/abs (v/- corner-bottom-right fess-bottom-right))
                                                       :angle 180
                                                       :reversed? true
                                                       :joint-angle 90
                                                       :render-options render-options)
        {line-pale-bottom-right :line
         line-pale-bottom-right-start :line-start
         :as line-pale-bottom-right-data} (line/create line
                                                       (v/abs (v/- corner-bottom-right pale-bottom-right))
                                                       :angle 90
                                                       :joint-angle 90
                                                       :render-options render-options)
        {line-pale-bottom-left :line
         line-pale-bottom-left-start :line-start
         :as line-pale-bottom-left-data} (line/create line
                                                      (v/abs (v/- corner-bottom-left pale-bottom-left))
                                                      :angle -90
                                                      :reversed? true
                                                      :joint-angle 90
                                                      :render-options render-options)
        {line-fess-bottom-left :line
         line-fess-bottom-left-start :line-start
         :as line-fess-bottom-left-data} (line/create line
                                                      (v/abs (v/- corner-bottom-left fess-bottom-left))
                                                      :angle 180
                                                      :joint-angle 90
                                                      :render-options render-options)
        {line-fess-top-left :line
         line-fess-top-left-start :line-start
         :as line-fess-top-left-data} (line/create line
                                                   (v/abs (v/- corner-top-left fess-top-left))
                                                   :reversed? true
                                                   :joint-angle 90
                                                   :render-options render-options)
        parts [[["M" (v/+ corner-top-left
                          line-pale-top-left-start)
                 (svg/stitch line-pale-top-left)
                 (infinity/path :clockwise
                                [:top :top]
                                [(v/+ pale-top-left
                                      line-pale-top-left-start)
                                 (v/+ pale-top-right
                                      line-pale-top-right-start)])
                 (svg/stitch line-pale-top-right)
                 "L" (v/+ corner-top-right
                          line-fess-top-right-start)
                 (svg/stitch line-fess-top-right)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/+ fess-top-right
                                      line-fess-top-right-start)
                                 (v/+ fess-bottom-right
                                      line-fess-bottom-right-start)])
                 (svg/stitch line-fess-bottom-right)
                 "L" (v/+ corner-bottom-right
                          line-pale-bottom-right-start)
                 (svg/stitch line-pale-bottom-right)
                 (infinity/path :clockwise
                                [:bottom :bottom]
                                [(v/+ pale-bottom-right
                                      line-pale-bottom-right-start)
                                 (v/+ pale-bottom-left
                                      line-pale-bottom-left-start)])
                 (svg/stitch line-pale-bottom-left)
                 "L" (v/+ corner-bottom-left
                          line-fess-bottom-left-start)
                 (svg/stitch line-fess-bottom-left)
                 (infinity/path :clockwise
                                [:left :left]
                                [(v/+ fess-bottom-left
                                      line-fess-bottom-left-start)
                                 (v/+ fess-top-left
                                      line-fess-top-left-start)])
                 (svg/stitch line-fess-top-left)
                 "z"]
                [top bottom left right]]]
        field (if (charge/counterchangable? field parent)
                (charge/counterchange-field field parent)
                field)
        [fimbriation-elements-1 fimbriation-outlines-1] (fimbriation/render
                                                         [fess-top-left :left]
                                                         [pale-top-left :top]
                                                         [line-fess-top-left-data
                                                          line-pale-top-left-data]
                                                         (:fimbriation line)
                                                         render-options)
        [fimbriation-elements-2 fimbriation-outlines-2] (fimbriation/render
                                                         [pale-top-right :top]
                                                         [fess-top-right :right]
                                                         [line-pale-top-right-data
                                                          line-fess-top-right-data]
                                                         (:fimbriation line)
                                                         render-options)
        [fimbriation-elements-3 fimbriation-outlines-3] (fimbriation/render
                                                         [fess-bottom-right :right]
                                                         [pale-bottom-right :bottom]
                                                         [line-fess-bottom-right-data
                                                          line-pale-bottom-right-data]
                                                         (:fimbriation line)
                                                         render-options)
        [fimbriation-elements-4 fimbriation-outlines-4] (fimbriation/render
                                                         [pale-bottom-left :bottom]
                                                         [fess-bottom-left :left]
                                                         [line-pale-bottom-left-data
                                                          line-fess-bottom-left-data]
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
                     ["M" (v/+ corner-top-left
                               line-pale-top-left-start)
                      (svg/stitch line-pale-top-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ pale-top-right
                               line-pale-top-right-start)
                      (svg/stitch line-pale-top-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top-right
                               line-fess-top-right-start)
                      (svg/stitch line-fess-top-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ fess-bottom-right
                               line-fess-bottom-right-start)
                      (svg/stitch line-fess-bottom-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom-right
                               line-pale-bottom-right-start)
                      (svg/stitch line-pale-bottom-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ pale-bottom-left
                               line-pale-bottom-left-start)
                      (svg/stitch line-pale-bottom-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom-left
                               line-fess-bottom-left-start)
                      (svg/stitch line-fess-bottom-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ fess-top-left
                               line-fess-top-left-start)
                      (svg/stitch line-fess-top-left)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2
         fimbriation-outlines-3
         fimbriation-outlines-4])
      environment ordinary context]]))
