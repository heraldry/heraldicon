(ns heraldry.coat-of-arms.ordinary.type.bend
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
  {:display-name "Bend"
   :value        :bend}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                geometry]}                       (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                           geometry
        opposite-line                            (ordinary-options/sanitize-opposite-line ordinary line)
        points                                   (:points environment)
        top                                      (:top points)
        bottom                                   (:bottom points)
        height                                   (:height environment)
        band-height                              (-> size
                                                     ((util/percent-of height)))
        {origin-point :real-origin
         anchor-point :real-anchor}              (angle/calculate-origin-and-anchor
                                                  environment
                                                  origin
                                                  anchor
                                                  band-height
                                                  nil)
        center-point                             (v/line-intersection origin-point anchor-point
                                                                      top bottom)
        direction                                (v/- anchor-point origin-point)
        direction                                (-> (v/v (-> direction :x Math/abs)
                                                          (-> direction :y Math/abs))
                                                     v/normal)
        direction-orthogonal                     (v/orthogonal direction)
        [middle-real-start
         middle-real-end]                        (v/environment-intersections
                                                  origin-point
                                                  (v/+ origin-point direction)
                                                  environment)
        band-length                              (-> (v/- middle-real-start center-point)
                                                     v/abs
                                                     (* 2))
        middle-start                             (-> direction
                                                     (v/* -30)
                                                     (v/+ middle-real-start))
        middle-end                               (-> direction
                                                     (v/* 30)
                                                     (v/+ middle-real-end))
        width-offset                             (-> direction-orthogonal
                                                     (v/* band-height)
                                                     (v// 2))
        ordinary-top-left                        (v/+ middle-real-start width-offset)
        first-start                              (v/+ middle-start width-offset)
        first-end                                (v/+ middle-end width-offset)
        second-start                             (v/- middle-start width-offset)
        second-end                               (v/- middle-end width-offset)
        [first-real-start
         first-real-end]                         (v/environment-intersections
                                                  first-start
                                                  first-end
                                                  environment)
        [second-real-start
         second-real-end]                        (v/environment-intersections
                                                  second-start
                                                  second-end
                                                  environment)
        real-start                               (min (-> (v/- first-real-start first-start)
                                                          (v/abs))
                                                      (-> (v/- second-real-start second-start)
                                                          (v/abs)))
        real-end                                 (max (-> (v/- first-real-start first-start)
                                                          (v/abs))
                                                      (-> (v/- second-real-end second-start)
                                                          (v/abs)))
        angle                                    (v/angle-to-point middle-start middle-end)
        line                                     (-> line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line                            (-> opposite-line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of height))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}           (line/create line
                                                              first-start
                                                              first-end
                                                              :real-start real-start
                                                              :real-end real-end
                                                              :render-options render-options
                                                              :environment environment)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data} (line/create opposite-line
                                                              second-start
                                                              second-end
                                                              :reversed? true
                                                              :real-start real-start
                                                              :real-end real-end
                                                              :render-options render-options
                                                              :environment environment)
        counterchanged?                          (counterchange/counterchangable? field parent)
        use-parent-environment?                  (or counterchanged?
                                                     (:inherit-environment? field))
        parts                                    [[["M" (v/+ first-start
                                                             line-one-start)
                                                    (svg/stitch line-one)
                                                    "L" (v/+ second-end
                                                             line-reversed-start)
                                                    (svg/stitch line-reversed)
                                                    "L" (v/+ first-start
                                                             line-one-start)
                                                    "z"]
                                                   (if use-parent-environment?
                                                     [first-real-start first-real-end
                                                      second-real-start second-real-end]
                                                     [(v/v 0 0)
                                                      (v/v band-length band-height)])]]
        field                                    (if counterchanged?
                                                   (counterchange/counterchange-field field parent)
                                                   field)
        outline?                                 (or (:outline? render-options)
                                                     (:outline? hints))]
    [:<>
     [division-shared/make-division
      :ordinary-fess [field] parts
      [:all]
      environment
      ordinary
      (-> context
          (assoc :transform (when (not use-parent-environment?)
                              (str "translate(" (v/->str ordinary-top-left) ")"
                                   "rotate(" angle ")"))))]
     (line/render line [line-one-data] first-start outline? render-options)
     (line/render opposite-line [line-reversed-data] second-end outline? render-options)]))

