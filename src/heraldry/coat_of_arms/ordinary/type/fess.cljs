(ns heraldry.coat-of-arms.ordinary.type.fess
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
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
  {:display-name "Fess"
   :value        :fess}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}           (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                           geometry
        opposite-line                            (ordinary-options/sanitize-opposite-line ordinary line)
        points                                   (:points environment)
        origin-point                             (position/calculate origin environment :fess)
        left                                     (assoc (:left points) :y (:y origin-point))
        right                                    (assoc (:right points) :y (:y origin-point))
        height                                   (:height environment)
        band-height                              (-> size
                                                     ((util/percent-of height)))
        row1                                     (- (:y origin-point) (/ band-height 2))
        row2                                     (+ row1 band-height)
        line                                     (-> line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line                            (-> opposite-line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}           (line/create line
                                                              (:x (v/- right left))
                                                              :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data} (line/create opposite-line
                                                              (:x (v/- right left))
                                                              :reversed? true
                                                              :angle 180
                                                              :render-options render-options)
        first-left                               (v/v (:x left) row1)
        first-right                              (v/v (:x right) row1)
        second-left                              (v/v (:x left) row2)
        second-right                             (v/v (:x right) row2)
        parts                                    [[["M" (v/+ first-left
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
                                                   [(v/+ first-right
                                                         line-one-start)
                                                    (v/+ second-left
                                                         line-reversed-start)]]]
        field                                    (if (counterchange/counterchangable? field parent)
                                                   (counterchange/counterchange-field field parent)
                                                   field)
        outline?                                 (or (:outline? render-options)
                                                     (:outline? hints))]
    [:<>
     (fimbriation/draw-line line line-one-data first-left outline? render-options)
     (fimbriation/draw-line opposite-line line-reversed-data second-right outline? render-options)
     [division-shared/make-division
      :ordinary-fess [field] parts
      [:all]
      environment ordinary context]
     (when outline?
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ first-left line-one-start)
                     (svg/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ second-right line-reversed-start)
                     (svg/stitch line-reversed)])}]])]))
