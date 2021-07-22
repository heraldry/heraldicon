(ns heraldry.coat-of-arms.ordinary.type.gore
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]))

(defn arm-diagonal [origin-point anchor-point]
  (-> (v/- anchor-point origin-point)
      v/normal
      (v/* 200)))

(def ordinary-type :heraldry.ordinary.type/gore)

(defmethod ordinary-interface/display-name ordinary-type [_] "Gore")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        opposite-line (interface/get-sanitized-data (conj path :opposite-line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        anchor (interface/get-sanitized-data (conj path :anchor) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))

        points (:points environment)
        left? (case (-> anchor :point)
                :top-left true
                :angle (-> anchor :angle neg?)
                false)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     -90)
        bottom (:bottom points)
        relative-arm (arm-diagonal origin-point anchor-point)
        diagonal-top (v/+ origin-point relative-arm)
        [_ intersection-top] (v/environment-intersections origin-point diagonal-top environment)
        flipped? (not left?)
        {line-diagonal :line
         line-diagonal-start :line-start
         :as line-diagonal-data} (line/create line
                                              origin-point diagonal-top
                                              :real-start 0
                                              :real-end (-> (v/- intersection-top origin-point)
                                                            v/abs)
                                              :flipped? flipped?
                                              :reversed? true
                                              :context context
                                              :environment environment)
        {line-down :line
         line-down-end :line-end
         :as line-down-data} (line/create opposite-line
                                          origin-point bottom
                                          :flipped? flipped?
                                          :real-start 0
                                          :real-end (-> (v/- bottom origin-point)
                                                        v/abs)
                                          :context context
                                          :environment environment)
        part [["M" (v/+ diagonal-top
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
               bottom]]
        ;; TODO: counterchanged
        ;; field (if (:counterchanged? field)
        ;;         (counterchange/counterchange-field ordinary parent)
        ;;         field)
        ]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     (line/render line [line-diagonal-data line-down-data] diagonal-top outline? context)]))
