(ns heraldry.coat-of-arms.ordinary.type.pale
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
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
  {:display-name "Pale"
   :value        :pale}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}           (options/sanitize ordinary (ordinary-options/options ordinary))
        opposite-line                            (ordinary-options/sanitize-opposite-line ordinary line)
        {:keys [size]}                           geometry
        points                                   (:points environment)
        origin-point                             (position/calculate origin environment :fess)
        top                                      (assoc (:top points) :x (:x origin-point))
        bottom                                   (assoc (:bottom points) :x (:x origin-point))
        width                                    (:width environment)
        band-width                               (-> size
                                                     ((util/percent-of width)))
        col1                                     (- (:x origin-point) (/ band-width 2))
        col2                                     (+ col1 band-width)
        [first-top first-bottom]                 (v/environment-intersections
                                                  (v/v col1 (:y top))
                                                  (v/v col1 (:y bottom))
                                                  environment)
        [second-top second-bottom]               (v/environment-intersections
                                                  (v/v col2 (:y top))
                                                  (v/v col2 (:y bottom))
                                                  environment)
        shared-start-y                           (- (min (:y first-top)
                                                         (:y second-top))
                                                    30)
        real-start                               (min (-> first-top :y (- shared-start-y))
                                                      (-> second-top :y (- shared-start-y)))
        real-end                                 (max (-> first-bottom :y (- shared-start-y))
                                                      (-> second-bottom :y (- shared-start-y)))
        shared-end-y                             (+ real-end 30)
        first-top                                (v/v (:x first-top) shared-start-y)
        second-top                               (v/v (:x second-top) shared-start-y)
        first-bottom                             (v/v (:x first-bottom) shared-end-y)
        second-bottom                            (v/v (:x second-bottom) shared-end-y)
        line                                     (-> line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of width))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of width)))
        opposite-line                            (-> opposite-line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of width))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of width)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}           (line/create line
                                                               first-top first-bottom
                                                               :reversed? true
                                                               :real-start real-start
                                                               :real-end real-end
                                                               :render-options render-options
                                                               :environment environment)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data} (line/create opposite-line
                                                               second-top second-bottom
                                                               :real-start real-start
                                                               :real-end real-end
                                                               :render-options render-options
                                                               :environment environment)
        parts                                    [[["M" (v/+ first-bottom
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
                                                   [first-bottom
                                                    second-top]]]
        field                                    (if (:counterchanged? field)
                                                   (counterchange/counterchange-field ordinary parent)
                                                   field)
        outline?                                 (or (:outline? render-options)
                                                     (:outline? hints))]
    [:<>
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-one-data] first-bottom outline? render-options)
     (line/render opposite-line [line-reversed-data] second-top outline? render-options)]))

