(ns heraldry.coat-of-arms.ordinary.type.bend-sinister
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.render-options :as render-options]
            [heraldry.util :as util]))

(defn render
  {:display-name "Bend sinister"
   :value :heraldry.ordinary.type/bend-sinister}
  [{:keys [field] :as ordinary} parent environment
   {:keys
    [render-options
     override-middle-real-start
     override-middle-real-end
     override-real-start
     override-real-end
     override-center-point] :as context}]
  (let [;; ignore offset constraints, because cottises might exceed them
        ordinary-options (-> (ordinary-options/options ordinary)
                             (assoc-in [:origin :offset-x :min] -100)
                             (assoc-in [:origin :offset-x :max] 100)
                             (assoc-in [:origin :offset-y :min] -100)
                             (assoc-in [:origin :offset-y :max] 100))
        {:keys [line opposite-line
                origin anchor geometry outline? cottising]} (options/sanitize ordinary ordinary-options)
        {:keys [size]} geometry
        points (:points environment)
        top (:top points)
        bottom (:bottom points)
        width (:width environment)
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     band-height
                                     nil)
        center-point (or override-center-point
                         (v/line-intersection origin-point anchor-point
                                              top bottom))
        direction (v/- anchor-point origin-point)
        direction (-> (v/v (-> direction :x Math/abs)
                           (-> direction :y Math/abs -))
                      v/normal)
        direction-orthogonal (v/orthogonal direction)
        [middle-real-start
         middle-real-end] (if (and override-middle-real-start
                                   override-middle-real-end)
                            [override-middle-real-start
                             override-middle-real-end]
                            (v/environment-intersections
                             origin-point
                             (v/+ origin-point direction)
                             environment))
        band-length (-> (v/- middle-real-end center-point)
                        v/abs
                        (* 2))
        middle-start (-> direction
                         (v/* -30)
                         (v/+ middle-real-start))
        middle-end (-> direction
                       (v/* 30)
                       (v/+ middle-real-end))
        width-offset (-> direction-orthogonal
                         (v/* band-height)
                         (v// 2))
        ordinary-top-left (-> middle-real-end
                              (v/+ width-offset)
                              (v/- (v/* direction band-length)))
        first-start (v/+ middle-start width-offset)
        first-end (v/+ middle-end width-offset)
        second-start (v/- middle-start width-offset)
        second-end (v/- middle-end width-offset)
        [first-real-start
         first-real-end] (v/environment-intersections
                          first-start
                          first-end
                          environment)
        [second-real-start
         second-real-end] (v/environment-intersections
                           second-start
                           second-end
                           environment)
        real-start (or override-real-start
                       (min (-> (v/- first-real-start first-start)
                                (v/abs))
                            (-> (v/- second-real-start second-start)
                                (v/abs))))
        real-end (or override-real-end
                     (max (-> (v/- first-real-start first-start)
                              (v/abs))
                          (-> (v/- second-real-end second-start)
                              (v/abs))))
        angle (v/angle-to-point middle-start middle-end)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (util/percent-of height))
                          (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one :line
         line-one-start :line-start
         line-one-min :line-min
         :as line-one-data} (line/create line
                                         first-start
                                         first-end
                                         :real-start real-start
                                         :real-end real-end
                                         :render-options render-options
                                         :environment environment)
        {line-reversed :line
         line-reversed-start :line-start
         line-reversed-min :line-min
         :as line-reversed-data} (line/create opposite-line
                                              second-start
                                              second-end
                                              :reversed? true
                                              :real-start real-start
                                              :real-end real-end
                                              :render-options render-options
                                              :environment environment)
        counterchanged? (:counterchanged? field)
        use-parent-environment? (or counterchanged?
                                    (:inherit-environment? field))
        parts [[["M" (v/+ first-start
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
        field (if counterchanged?
                (counterchange/counterchange-field ordinary parent)
                field)
        [render-options-outline?] (options/effective-values [[:outline?]] render-options render-options/options)
        outline? (or render-options-outline?
                     outline?)
        {:keys [cottise-1
                cottise-2
                cottise-opposite-1
                cottise-opposite-2]} (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfields
      :ordinary-fess [field] parts
      [:all]
      environment
      ordinary
      (-> context
          (assoc :transform (when (not use-parent-environment?)
                              (str "translate(" (v/->str ordinary-top-left) ")"
                                   "rotate(" angle ")"))))]
     (line/render line [line-one-data] first-start outline? render-options)
     (line/render opposite-line [line-reversed-data] second-end outline? render-options)
     (when (:enabled? cottise-1)
       (let [cottise-1-data (:cottise-1 cottising)
             dist (-> (+ (:distance cottise-1-data))
                      (+ (/ (:thickness cottise-1-data) 2))
                      (/ 100)
                      (* height)
                      (+ (/ band-height 2))
                      (- line-one-min))
             point-offset (v/* direction-orthogonal dist)
             new-center-point (v/+ center-point point-offset)
             fess-offset (v/- new-center-point (get points :fess))
             new-origin {:point :fess
                         :offset-x (-> fess-offset
                                       :x
                                       (/ width)
                                       (* 100))
                         :offset-y (-> fess-offset
                                       :y
                                       (/ height)
                                       (* 100)
                                       -)
                         :alignment :right}
             new-anchor {:point :angle
                         :angle (- angle)}]
         [render (-> ordinary
                     (assoc :cottising {:cottise-1 cottise-2})
                     (assoc :line (:line cottise-1))
                     (assoc :opposite-line (:opposite-line cottise-1))
                     (assoc :field (:field cottise-1))
                     (assoc-in [:geometry :size] (:thickness cottise-1-data))
                     (assoc :origin new-origin)
                     (assoc :anchor new-anchor)) parent environment
          (-> context
              (assoc :override-center-point new-center-point)
              (assoc :override-middle-real-start (v/+ middle-real-start point-offset))
              (assoc :override-middle-real-end (v/+ middle-real-end point-offset))
              (assoc :override-real-start real-start)
              (assoc :override-real-end real-end))]))
     (when (:enabled? cottise-opposite-1)
       (let [cottise-opposite-1-data (:cottise-opposite-1 cottising)
             bend-sinister-base {:type :heraldry.ordinary.type/bend-sinister
                                 :line (:line cottise-opposite-1)
                                 :opposite-line (:opposite-line cottise-opposite-1)}
             bend-sinister-options (ordinary-options/options bend-sinister-base)
             {:keys [line
                     opposite-line]} (options/sanitize bend-sinister-base bend-sinister-options)
             dist (-> (+ (:distance cottise-opposite-1-data))
                      (+ (/ (:thickness cottise-opposite-1-data) 2))
                      (/ 100)
                      (* height)
                      (+ (/ band-height 2))
                      (- line-reversed-min))
             point-offset (v/* direction-orthogonal dist)
             new-center-point (v/- center-point point-offset)
             fess-offset (v/- new-center-point (get points :fess))
             new-origin {:point :fess
                         :offset-x (-> fess-offset
                                       :x
                                       (/ width)
                                       (* 100))
                         :offset-y (-> fess-offset
                                       :y
                                       (/ height)
                                       (* 100)
                                       -)
                         :alignment :left}
             new-anchor {:point :angle
                         :angle (- angle)}]
         [render (-> ordinary
                     (assoc :cottising {:cottise-opposite-1 cottise-opposite-2})
                     ;; swap line/opposite-line because the cottise fess is upside down
                     (assoc :line opposite-line)
                     (assoc :opposite-line line)
                     (assoc :field (:field cottise-opposite-1))
                     (assoc-in [:geometry :size] (:thickness cottise-opposite-1-data))
                     (assoc :origin new-origin)
                     (assoc :anchor new-anchor)) parent environment
          (-> context
              (assoc :override-center-point new-center-point)
              (assoc :override-middle-real-start (v/- middle-real-start point-offset))
              (assoc :override-middle-real-end (v/- middle-real-end point-offset))
              (assoc :override-real-start real-start)
              (assoc :override-real-end real-end))]))]))
