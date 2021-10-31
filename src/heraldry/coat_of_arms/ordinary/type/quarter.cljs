(ns heraldry.coat-of-arms.ordinary.type.quarter
  (:require
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]))

(def ordinary-type :heraldry.ordinary.type/quarter)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Quarter / Canton"
                                                              :de "Vierung / Obereck"})

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [path environment] :as context}]
  (let [line (interface/get-sanitized-data (update context :path conj :line))
        opposite-line (interface/get-sanitized-data (update context :path conj :opposite-line))
        variant (interface/get-sanitized-data (update context :path conj :variant))
        origin (interface/get-sanitized-data (update context :path conj :origin))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (update context :path conj :outline?)))
        size (interface/get-sanitized-data (update context :path conj :geometry :size))
        points (:points environment)
        width (:width environment)
        height (:height environment)
        origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x origin-point))
        top-left (:top-left points)
        top-right (:top-right points)
        bottom (assoc (:bottom points) :x (:x origin-point))
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        target-part-index (get {:dexter-chief 0
                                :sinister-chief 1
                                :dexter-base 2
                                :sinister-base 3} variant 0)
        relevant-corner (case target-part-index
                          0 top-left
                          1 top-right
                          2 bottom-left
                          3 bottom-right)
        origin-point (-> origin-point
                         (v/sub relevant-corner)
                         (v/mul (/ size 100))
                         (v/add relevant-corner))

        intersection-top (v/find-first-intersection-of-ray origin-point top environment)
        intersection-bottom (v/find-first-intersection-of-ray origin-point bottom environment)
        intersection-left (v/find-first-intersection-of-ray origin-point left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point right environment)
        arm-length (->> [(when (#{0 1} target-part-index)
                           intersection-top)
                         (when (#{2 3} target-part-index)
                           intersection-bottom)
                         (when (#{0 2} target-part-index)
                           intersection-left)
                         (when (#{1 3} target-part-index)
                           intersection-right)]
                        (filter identity)
                        (map #(-> %
                                  (v/sub origin-point)
                                  v/abs))
                        (apply max))
        full-arm-length (+ arm-length 30)
        point-top (-> (v/v 0 -1)
                      (v/mul full-arm-length)
                      (v/add origin-point))
        point-bottom (-> (v/v 0 1)
                         (v/mul full-arm-length)
                         (v/add origin-point))
        point-left (-> (v/v -1 0)
                       (v/mul full-arm-length)
                       (v/add origin-point))
        point-right (-> (v/v 1 0)
                        (v/mul full-arm-length)
                        (v/add origin-point))
        {line-top :line
         line-top-start :line-start
         :as line-top-data
         line-top-min :line-min} (line/create line
                                              origin-point point-top
                                              :reversed? true
                                              :real-start 0
                                              :real-end arm-length
                                              :context context
                                              :environment environment)
        {line-right :line
         line-right-start :line-start
         :as line-right-data} (line/create opposite-line
                                           origin-point point-right
                                           :flipped? true
                                           :mirrored? true
                                           :real-start 0
                                           :real-end arm-length
                                           :context context
                                           :environment environment)
        {line-bottom :line
         line-bottom-start :line-start
         :as line-bottom-data} (line/create line
                                            origin-point point-bottom
                                            :reversed? true
                                            :real-start 0
                                            :real-end arm-length
                                            :context context
                                            :environment environment)
        {line-left :line
         line-left-start :line-start
         :as line-left-data} (line/create opposite-line
                                          origin-point point-left
                                          :flipped? true
                                          :mirrored? true
                                          :real-start 0
                                          :real-end arm-length
                                          :context context
                                          :environment environment)
        parts [[["M" (v/add point-top
                            line-top-start)
                 (path/stitch line-top)
                 "L" origin-point
                 (path/stitch line-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [(v/add point-left
                                        line-left-start)
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [top-left origin-point]]

               [["M" (v/add point-top
                            line-top-start)
                 (path/stitch line-top)
                 "L" origin-point
                 (path/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/add point-right
                                        line-right-start)
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [origin-point top-right]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" origin-point
                 (path/stitch line-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [(v/add point-left
                                        line-left-start)
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [origin-point bottom-left]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" origin-point
                 (path/stitch line-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [(v/add point-right
                                        line-right-start)
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [origin-point bottom-right]]]
        [line-one-data
         line-two-data] (case target-part-index
                          0 [line-top-data line-left-data]
                          1 [line-top-data line-right-data]
                          2 [line-bottom-data line-left-data]
                          3 [line-bottom-data line-right-data])
        part (get parts target-part-index)]
    [:<>
     [field-shared/make-subfield
      (update context :path conj :field)
      part
      :all]
     [line/render line [line-one-data line-two-data] (case target-part-index
                                                       0 point-top
                                                       1 point-top
                                                       2 point-bottom
                                                       3 point-bottom) outline? context]
     [cottising/render-chevron-cottise
      :cottise-1 :cottise-2 :cottise-1
      path environment context
      :distance-fn (fn [distance _]
                     (-> (- distance)
                         (/ 100)
                         (* size)
                         (+ line-top-min)))
      :alignment :left
      :width width
      :height height
      :chevron-angle (case target-part-index
                       0 225
                       1 315
                       2 135
                       3 45)
      :joint-angle 90
      :corner-point origin-point]]))
