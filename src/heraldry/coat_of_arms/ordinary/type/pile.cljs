(ns heraldry.coat-of-arms.ordinary.type.pile
  (:require [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.ordinary.type.chevron :as chevron]
            [heraldry.coat-of-arms.shared.pile :as pile]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Pile"
   :value :heraldry.ordinary.type/pile}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin anchor
                geometry]} (options/sanitize ordinary (ordinary-options/options ordinary))
        opposite-line (ordinary-options/sanitize-opposite-line ordinary line)
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        width (:width environment)
        height (:height environment)
        thickness-base (if (#{:left :right} (:point origin))
                         height
                         width)
        {origin-point :origin
         point :point
         thickness :thickness} (pile/calculate-properties
                                environment
                                origin
                                (cond-> anchor
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point origin)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                thickness-base
                                (case (:point origin)
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        pile-angle (v/angle-to-point point origin-point)
        {left-point :left
         right-point :right} (pile/diagonals origin-point point thickness)
        intersection-left (-> (v/environment-intersections point left-point environment)
                              last)
        intersection-right (-> (v/environment-intersections point right-point environment)
                               last)
        joint-angle (v/angle-between-vectors (v/- intersection-left point)
                                             (v/- intersection-right point))
        end-left (-> intersection-left
                     (v/- point)
                     v/abs)
        end-right (-> intersection-right
                      (v/- point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of thickness-base))
                 (update-in [:fimbriation :thickness-2] (util/percent-of thickness-base)))
        {line-left :line
         line-left-start :line-start
         line-left-min :line-min
         :as line-left-data} (line/create line
                                          point left-point
                                          :reversed? true
                                          :real-start 0
                                          :real-end end
                                          :render-options render-options
                                          :environment environment)
        {line-right :line
         :as line-right-data} (line/create opposite-line
                                           point right-point
                                           :real-start 0
                                           :real-end end
                                           :render-options render-options
                                           :environment environment)
        parts [[["M" (v/+ left-point
                          line-left-start)
                 (svg/stitch line-left)
                 (svg/stitch line-right)
                 "z"]
                [top-left top-right
                 bottom-left bottom-right]]]
        field (if (:counterchanged? field)
                (counterchange/counterchange-field ordinary parent)
                field)
        outline? (or (:outline? render-options)
                     (:outline? hints))
        {:keys [cottise-1
                cottise-2]} (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfields
      :ordinary-pile [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-left-data
                        line-right-data] left-point outline? render-options)
     (when (:enabled? cottise-1)
       (let [cottise-1-data (options/sanitize cottise-1 cottising/cottise-options)
             half-joint-angle (/ joint-angle 2)
             half-joint-angle-rad (-> half-joint-angle
                                      (/ 180)
                                      (* Math/PI)
                                      Math/sin)
             dist (-> (+ (:distance cottise-1-data))
                      (/ 100)
                      (* thickness-base)
                      (- line-left-min)
                      (/ (if (zero? half-joint-angle)
                           0.00001
                           (Math/sin half-joint-angle-rad))))
             line-offset (-> half-joint-angle-rad
                             Math/cos
                             (* dist)
                             (/ (-> cottise-1 :opposite-line :width)))
             point-offset (-> (v/v (- dist) 0)
                              (v/rotate pile-angle)
                              (v/+ point))
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
                                   :angle (- pile-angle 90)}]
         [chevron/render (-> {:type :heraldry.ordinary.type/chevron
                              :hints {:outline? (-> ordinary :hints :outline?)}}
                             (assoc :cottising {:cottise-1 cottise-2})
                             (assoc :line (:line cottise-1))
                             (assoc :opposite-line (-> (:opposite-line cottise-1)
                                                       (assoc :offset line-offset)))
                             (assoc :field (:field cottise-1))
                             (assoc-in [:geometry :size] (:thickness cottise-1-data))
                             (assoc :origin new-origin)
                             (assoc :direction-anchor new-direction-anchor)
                             (assoc :anchor new-anchor)) parent environment
          context]))]))
