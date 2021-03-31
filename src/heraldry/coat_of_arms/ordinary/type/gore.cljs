(ns heraldry.coat-of-arms.ordinary.type.gore
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn arm-diagonal [origin-point anchor-point]
  (-> (v/- anchor-point origin-point)
      v/normal
      (v/* 200)))

(defn render
  {:display-name "Gore"
   :value        :gore}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor]}             (options/sanitize ordinary (ordinary-options/options ordinary))
        opposite-line                            (ordinary-options/sanitize-opposite-line ordinary line)
        points                                   (:points environment)
        left?                                    (case (-> anchor :point)
                                                   :top-left true
                                                   :angle    (-> anchor :angle neg?)
                                                   false)
        {origin-point :real-origin
         anchor-point :real-anchor}              (angle/calculate-origin-and-anchor
                                                  environment
                                                  origin
                                                  anchor
                                                  0
                                                  -90)
        bottom                                   (:bottom points)
        relative-arm                             (arm-diagonal origin-point anchor-point)
        diagonal-top                             (v/+ origin-point relative-arm)
        [_ intersection-top]                     (v/environment-intersections origin-point diagonal-top environment)
        flipped?                                 (not left?)
        {line-diagonal       :line
         line-diagonal-start :line-start
         :as                 line-diagonal-data} (line/create line
                                                              origin-point diagonal-top
                                                              :real-start 0
                                                              :real-end (-> (v/- intersection-top origin-point)
                                                                            v/abs)
                                                              :flipped? flipped?
                                                              :reversed? true
                                                              :render-options render-options
                                                              :environment environment)
        {line-down     :line
         line-down-end :line-end
         :as           line-down-data}           (line/create opposite-line
                                                              origin-point bottom
                                                              :flipped? flipped?
                                                              :real-start 0
                                                              :real-end (-> (v/- bottom origin-point)
                                                                            v/abs)
                                                              :render-options render-options
                                                              :environment environment)
        parts                                    [[["M" (v/+ diagonal-top
                                                             line-diagonal-start)
                                                    (svg/stitch line-diagonal)
                                                    "L" origin-point
                                                    (svg/stitch line-down)
                                                    (infinity/path (if left?
                                                                     :clockwise
                                                                     :counter-clockwise)
                                                                   [:bottom :top]
                                                                   [(v/+ bottom
                                                                         line-down-end)
                                                                    (v/+ diagonal-top
                                                                         line-diagonal-start)])
                                                    "z"]
                                                   [intersection-top
                                                    origin-point
                                                    bottom]]]
        field                                    (if (:counterchanged? field)
                                                   (counterchange/counterchange-field ordinary parent)
                                                   field)
        outline?                                 (or (:outline? render-options)
                                                     (:outline? hints))]
    [:<>
     [division-shared/make-division
      :ordinary-base [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-diagonal-data line-down-data] diagonal-top outline? render-options)]))

