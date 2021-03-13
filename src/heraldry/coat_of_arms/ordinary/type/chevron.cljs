(ns heraldry.coat-of-arms.ordinary.type.chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn arm-diagonals [variant origin-point anchor-point]
  (let [direction    (-> (v/- anchor-point origin-point)
                         v/normal
                         (v/* 200))
        [left right] (case variant
                       :base     (if (-> direction :x (> 0))
                                   [(v/v -1 1) (v/v 1 1)]
                                   [(v/v 1 1) (v/v -1 1)])
                       :chief    (if (-> direction :x (< 0))
                                   [(v/v -1 1) (v/v 1 1)]
                                   [(v/v 1 1) (v/v -1 1)])
                       :dexter   (if (-> direction :y (< 0))
                                   [(v/v 1 -1) (v/v 1 1)]
                                   [(v/v 1 1) (v/v 1 -1)])
                       :sinister (if (-> direction :y (> 0))
                                   [(v/v 1 -1) (v/v 1 1)]
                                   [(v/v 1 1) (v/v 1 -1)]))]
    [(v/dot direction left)
     (v/dot direction right)]))

(defn sanitize-anchor [variant anchor]
  (let [[allowed default] (case variant
                            :base     [#{:angle :bottom-right} :bottom-left]
                            :chief    [#{:angle :top-right} :top-left]
                            :dexter   [#{:angle :bottom-left} :top-left]
                            :sinister [#{:angle :bottom-right} :top-right])]
    (update anchor :point #(or (allowed %) default))))

(defn mirror-point [variant center point]
  (-> point
      (v/- center)
      (v/dot (if (#{:base :chief} variant)
               (v/v -1 1)
               (v/v 1 -1)))
      (v/+ center)))

(defn render
  {:display-name "Chevron"
   :value        :chevron}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                variant geometry]}                     (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]}                                 geometry
        opposite-line                                  (ordinary-options/sanitize-opposite-line ordinary line)
        points                                         (:points environment)
        unadjusted-origin-point                        (position/calculate origin environment)
        top-left                                       (:top-left points)
        top-right                                      (:top-right points)
        bottom-left                                    (:bottom-left points)
        bottom-right                                   (:bottom-right points)
        top                                            (assoc (:top points) :x (:x unadjusted-origin-point))
        bottom                                         (assoc (:bottom points) :x (:x unadjusted-origin-point))
        left                                           (assoc (:left points) :y (:y unadjusted-origin-point))
        right                                          (assoc (:right points) :y (:y unadjusted-origin-point))
        height                                         (:height environment)
        band-width                                     (-> size
                                                           ((util/percent-of height)))
        anchor                                         (sanitize-anchor variant anchor)
        {origin-point :real-origin
         anchor-point :real-anchor}                    (angle/calculate-origin-and-anchor
                                                        environment
                                                        origin
                                                        anchor
                                                        band-width
                                                        (case variant
                                                          :base   90
                                                          :chief  -90
                                                          :dexter 180
                                                          0))
        [mirrored-origin mirrored-anchor]              [(mirror-point variant unadjusted-origin-point origin-point)
                                                        (mirror-point variant unadjusted-origin-point anchor-point)]
        origin-point                                   (v/line-intersection origin-point anchor-point
                                                                            mirrored-origin mirrored-anchor)
        [relative-left relative-right]                 (arm-diagonals variant origin-point anchor-point)
        diagonal-left                                  (v/+ origin-point relative-left)
        diagonal-right                                 (v/+ origin-point relative-right)
        angle-left                                     (angle/angle-to-point origin-point diagonal-left)
        angle-right                                    (angle/angle-to-point origin-point diagonal-right)
        joint-angle                                    (- angle-left angle-right)
        middle-angle                                   (-> (/ (+ angle-left angle-right) 2)
                                                           (* Math/PI) (/ 180))
        delta                                          (/ band-width 2 (Math/sin (-> joint-angle
                                                                                     (* Math/PI)
                                                                                     (/ 180)
                                                                                     (/ 2))))
        offset-lower                                   (v/v (* (Math/cos middle-angle)
                                                               delta)
                                                            (* (Math/sin middle-angle)
                                                               delta))
        offset-upper                                   (v/v (* (Math/cos middle-angle)
                                                               (- delta))
                                                            (* (Math/sin middle-angle)
                                                               (- delta)))
        corner-upper                                   (v/+ origin-point offset-upper)
        corner-lower                                   (v/+ origin-point offset-lower)
        left-upper                                     (v/+ diagonal-left offset-upper)
        left-lower                                     (v/+ diagonal-left offset-lower)
        right-upper                                    (v/+ diagonal-right offset-upper)
        right-lower                                    (v/+ diagonal-right offset-lower)
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
                                                          "L" (v/+ right-lower
                                                                   line-right-lower-start)
                                                          (svg/stitch line-right-lower)
                                                          "L" (v/+ corner-lower
                                                                   line-left-lower-start)
                                                          (svg/stitch line-left-lower)
                                                          "L" (v/+ left-upper
                                                                   line-left-upper-start)
                                                          (svg/stitch line-left-upper)
                                                          "z"]
                                                         (-> [corner-upper corner-lower]
                                                             (concat (case variant
                                                                       :chief    [top-left top-right]
                                                                       :dexter   [top-left bottom-left]
                                                                       :sinister [top-right bottom-right]
                                                                       [bottom-left bottom-right]))
                                                             vec)]]
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
