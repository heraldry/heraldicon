(ns heraldry.coat-of-arms.ordinary.type.chevron
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.shared.chevron :as chevron]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.render-options :as render-options]
            [heraldry.util :as util]))

(defn render
  {:display-name "Chevron"
   :value :heraldry.ordinary.type/chevron}
  [{:keys [field] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [;; ignore offset constraints, because cottises might exceed them
        ordinary-options (-> (ordinary-options/options ordinary)
                             (assoc-in [:origin :offset-x :min] -1000)
                             (assoc-in [:origin :offset-x :max] 1000)
                             (assoc-in [:origin :offset-y :min] -1000)
                             (assoc-in [:origin :offset-y :max] 1000)
                             (assoc-in [:direction-anchor :angle :min] -360)
                             (assoc-in [:direction-anchor :angle :max] 360)
                             (assoc-in [:anchor :angle :min] -360)
                             (assoc-in [:anchor :angle :max] 360))
        {:keys [line opposite-line origin anchor
                direction-anchor
                geometry outline? cottising]} (options/sanitize ordinary ordinary-options)
        raw-direction-anchor (:direction-anchor ordinary)
        direction-anchor (options/sanitize (cond-> raw-direction-anchor
                                             (-> direction-anchor
                                                 :point
                                                 #{:left
                                                   :right
                                                   :top
                                                   :bottom}) (->
                                                              (update :offset-x #(or % (:offset-x origin)))
                                                              (update :offset-y #(or % (:offset-y origin)))))
                                           (:direction-anchor ordinary-options))
        {:keys [size]} geometry
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of height)))
        {direction-origin-point :real-origin
         direction-anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                               environment
                                               origin
                                               direction-anchor
                                               0
                                               90)
        chevron-angle (v/normalize-angle
                       (v/angle-to-point direction-origin-point
                                         direction-anchor-point))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     band-width
                                     chevron-angle)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point chevron-angle unadjusted-origin-point origin-point)
                                           (chevron/mirror-point chevron-angle unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle origin-point anchor-point)
        diagonal-left (v/+ origin-point relative-left)
        diagonal-right (v/+ origin-point relative-right)
        angle-left (v/normalize-angle (v/angle-to-point origin-point diagonal-left))
        angle-right (v/normalize-angle (v/angle-to-point origin-point diagonal-right))
        joint-angle (v/normalize-angle (- angle-left angle-right))
        middle-angle (-> chevron-angle
                         (* Math/PI) (/ 180))
        delta (/ band-width 2 (Math/sin (-> joint-angle
                                            (* Math/PI)
                                            (/ 180)
                                            (/ 2))))
        offset-lower (v/v (* (Math/cos middle-angle)
                             delta)
                          (* (Math/sin middle-angle)
                             delta))
        offset-upper (v/v (* (Math/cos middle-angle)
                             (- delta))
                          (* (Math/sin middle-angle)
                             (- delta)))
        corner-upper (v/+ origin-point offset-upper)
        corner-lower (v/+ origin-point offset-lower)
        left-upper (v/+ diagonal-left offset-upper)
        left-lower (v/+ diagonal-left offset-lower)
        right-upper (v/+ diagonal-right offset-upper)
        right-lower (v/+ diagonal-right offset-lower)
        intersection-left-upper (v/find-first-intersection-of-ray corner-upper left-upper environment)
        intersection-right-upper (v/find-first-intersection-of-ray corner-upper right-upper environment)
        intersection-left-lower (v/find-first-intersection-of-ray corner-lower left-lower environment)
        intersection-right-lower (v/find-first-intersection-of-ray corner-lower right-lower environment)
        end-left-upper (-> intersection-left-upper
                           (v/- corner-upper)
                           v/abs)
        end-right-upper (-> intersection-right-upper
                            (v/- corner-upper)
                            v/abs)
        end-left-lower (-> intersection-left-lower
                           (v/- corner-lower)
                           v/abs)
        end-right-lower (-> intersection-right-lower
                            (v/- corner-lower)
                            v/abs)
        end (max end-left-upper
                 end-right-upper
                 end-left-lower
                 end-right-lower)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (util/percent-of height))
                          (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-right-upper :line
         line-right-upper-start :line-start
         line-right-upper-min :line-min
         :as line-right-upper-data} (line/create line
                                                 corner-upper right-upper
                                                 :real-start 0
                                                 :real-end end
                                                 :render-options render-options
                                                 :environment environment)
        {line-right-lower :line
         line-right-lower-start :line-start
         line-right-lower-min :line-min
         :as line-right-lower-data} (line/create opposite-line
                                                 corner-lower right-lower
                                                 :reversed? true
                                                 :real-start 0
                                                 :real-end end
                                                 :render-options render-options
                                                 :environment environment)
        {line-left-lower :line
         line-left-lower-start :line-start
         :as line-left-lower-data} (line/create opposite-line
                                                corner-lower left-lower
                                                :real-start 0
                                                :real-end end
                                                :render-options render-options
                                                :environment environment)
        {line-left-upper :line
         line-left-upper-start :line-start
         :as line-left-upper-data} (line/create line
                                                corner-upper left-upper
                                                :reversed? true
                                                :real-start 0
                                                :real-end end
                                                :render-options render-options
                                                :environment environment)
        parts [[["M" (v/+ corner-upper
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
                [top-left bottom-right]]]
        field (if (:counterchanged? field)
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
      :ordinary-pale [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-left-upper-data
                        line-right-upper-data] left-upper outline? render-options)
     (line/render opposite-line [line-right-lower-data
                                 line-left-lower-data] right-lower outline? render-options)
     (when (:enabled? cottise-1)
       (let [cottise-1-data (:cottise-1 cottising)
             chevron-base {:type :heraldry.ordinary.type/chevron
                           :line (:line cottise-1)
                           :opposite-line (:opposite-line cottise-1)}
             chevron-options (ordinary-options/options chevron-base)
             {:keys [line
                     opposite-line]} (options/sanitize chevron-base chevron-options)
             half-joint-angle (/ joint-angle 2)
             half-joint-angle-rad (-> half-joint-angle
                                      (/ 180)
                                      (* Math/PI)
                                      Math/sin)
             dist (-> (+ (:distance cottise-1-data))
                      (/ 100)
                      (* height)
                      (- line-right-upper-min)
                      (/ (if (zero? half-joint-angle)
                           0.00001
                           (Math/sin half-joint-angle-rad))))
             line-offset (or (-> cottise-1 :opposite-line :offset)
                             (-> half-joint-angle-rad
                                 Math/cos
                                 (* dist)
                                 (/ (:width opposite-line))))
             point-offset (-> (v/v (- dist) 0)
                              (v/rotate chevron-angle)
                              (v/+ corner-upper))
             fess-offset (v/- point-offset (get points :fess))
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
                         :angle half-joint-angle}
             new-direction-anchor {:point :angle
                                   :angle (- chevron-angle 90)}]
         [render (-> ordinary
                     (assoc :cottising {:cottise-1 cottise-2})
                     (assoc :line (:line cottise-1))
                     (assoc :opposite-line (-> (:opposite-line cottise-1)
                                               (assoc :offset line-offset)))
                     (assoc :field (:field cottise-1))
                     (assoc-in [:geometry :size] (:thickness cottise-1-data))
                     (assoc :origin new-origin)
                     (assoc :direction-anchor new-direction-anchor)
                     (assoc :anchor new-anchor)) parent environment
          context]))
     (when (:enabled? cottise-opposite-1)
       (let [cottise-opposite-1-data (:cottise-opposite-1 cottising)
             chevron-base {:type :heraldry.ordinary.type/chevron
                           :line (:line cottise-opposite-1)
                           :opposite-line (:opposite-line cottise-opposite-1)}
             chevron-options (ordinary-options/options chevron-base)
             {:keys [line
                     opposite-line]} (options/sanitize chevron-base chevron-options)
             half-joint-angle (/ joint-angle 2)
             half-joint-angle-rad (-> half-joint-angle
                                      (/ 180)
                                      (* Math/PI)
                                      Math/sin)
             dist (-> (+ (:distance cottise-opposite-1-data))
                      (/ 100)
                      (* height)
                      (- line-right-lower-min)
                      (/ (if (zero? half-joint-angle)
                           0.00001
                           (Math/sin half-joint-angle-rad))))
             point-offset (-> (v/v dist 0)
                              (v/rotate chevron-angle)
                              (v/+ corner-lower))
             fess-offset (v/- point-offset (get points :fess))
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
                         :angle half-joint-angle}
             new-direction-anchor {:point :angle
                                   :angle (- chevron-angle 90)}]
         [render (-> ordinary
                     (assoc :cottising {:cottise-opposite-1 cottise-opposite-2})
                     ;; swap line/opposite-line because the cottise fess is upside down
                     (assoc :line opposite-line)
                     (assoc :opposite-line line)
                     (assoc :field (:field cottise-opposite-1))
                     (assoc-in [:geometry :size] (:thickness cottise-opposite-1-data))
                     (assoc :origin new-origin)
                     (assoc :direction-anchor new-direction-anchor)
                     (assoc :anchor new-anchor)) parent environment
          context]))]))
