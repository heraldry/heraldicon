(ns heraldry.coat-of-arms.ordinary.type.bend-sinister
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.vector.core :as v]
            [heraldry.interface :as interface]
            [heraldry.svg :as svg]
            [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/bend-sinister)

(defmethod ordinary-interface/display-name ordinary-type [_] "Bend sinister")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment {:keys [override-middle-real-start
                                         override-middle-real-end
                                         override-real-start
                                         override-real-end
                                         override-center-point] :as context}]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        opposite-line (interface/get-sanitized-data (conj path :opposite-line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        anchor (interface/get-sanitized-data (conj path :anchor) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
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
                                         :context context
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
                                              :context context
                                              :environment environment)
        counterchanged? (interface/get-sanitized-data (conj path :field :counterchanged?) context)
        inherit-environment? (interface/get-sanitized-data (conj path :field :inherit-environment?) context)
        use-parent-environment? (or counterchanged?
                                    inherit-environment?)
        part [["M" (v/+ first-start
                        line-one-start)
               (svg/stitch line-one)
               "L" (v/+ second-end
                        line-reversed-start)
               (svg/stitch line-reversed)
               "L" (v/+ first-start
                        line-one-start)
               "z"]]
        cottise-context (merge
                         context
                         {:override-real-start real-start
                          :override-real-end real-end})]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment
      (-> context
          (assoc :transform (when (not use-parent-environment?)
                              (str "translate(" (v/->str ordinary-top-left) ")"
                                   "rotate(" angle ")"))))]
     [line/render line [line-one-data] first-start outline? context]
     [line/render opposite-line [line-reversed-data] second-end outline? context]
     [cottising/render-bend-cottise
      :cottise-1 :cottise-2 :cottise-1
      path environment cottise-context
      :sinister? true
      :distance-fn (fn [distance thickness]
                     (-> (+ distance)
                         (+ (/ thickness 2))
                         (/ 100)
                         (* height)
                         (+ (/ band-height 2))
                         (- line-one-min)))
      :alignment :right
      :width width
      :height height
      :angle (- angle)
      :direction-orthogonal direction-orthogonal
      :center-point center-point
      :middle-real-start-fn (fn [point-offset]
                              (v/+ middle-real-start point-offset))
      :middle-real-end-fn (fn [point-offset]
                            (v/+ middle-real-end point-offset))]
     [cottising/render-bend-cottise
      :cottise-opposite-1 :cottise-opposite-2 :cottise-opposite-1
      path environment cottise-context
      :sinister? true
      :distance-fn (fn [distance thickness]
                     (-> (+ distance)
                         (+ (/ thickness 2))
                         (/ 100)
                         (* height)
                         (+ (/ band-height 2))
                         (- line-reversed-min)))
      :alignment :left
      :width width
      :height height
      :angle (- angle)
      :direction-orthogonal direction-orthogonal
      :center-point center-point
      :middle-real-start-fn (fn [point-offset]
                              (v/- middle-real-start point-offset))
      :middle-real-end-fn (fn [point-offset]
                            (v/- middle-real-end point-offset))
      :swap-lines? true]]))
