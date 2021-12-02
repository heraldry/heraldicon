(ns heraldry.coat-of-arms.ordinary.type.quarter
  (:require
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.shared :as ordinary-shared]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]))

(def ordinary-type :heraldry.ordinary.type/quarter)

(defmethod ordinary-interface/display-name ordinary-type [_] (string "Quarter / Canton"))

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line))
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (-> {:origin {:point {:type :choice
                          :choices [[(string "Fess [point]") :fess]
                                    [(string "Chief [point]") :chief]
                                    [(string "Base [point]") :base]
                                    [(string "Honour [point]") :honour]
                                    [(string "Nombril [point]") :nombril]
                                    [(string "Top") :top]
                                    [(string "Bottom") :bottom]]
                          :default :fess
                          :ui {:label (string "Point")}}
                  :alignment {:type :choice
                              :choices position/alignment-choices
                              :default :middle
                              :ui {:label (string "Alignment")
                                   :form-type :radio-select}}
                  :offset-y {:type :range
                             :min -45
                             :max 45
                             :default 0
                             :ui {:label (string "Offset y")
                                  :step 0.1}}
                  :ui {:label (string "Origin")
                       :form-type :position}}
         :line line-style
         :opposite-line opposite-line-style
         :variant {:type :choice
                   :choices [[(string "Dexter-chief") :dexter-chief]
                             [(string "Sinister-chief") :sinister-chief]
                             [(string "Dexter-base") :dexter-base]
                             [(string "Sinister-base") :sinister-base]]
                   :default :dexter-chief
                   :ui {:label (string "Variant")
                        :form-type :select}}
         :geometry {:size {:type :range
                           :min 10
                           :max 150
                           :default 100
                           :ui {:label (string "Size")
                                :step 0.1}}
                    :ui {:label (string "Geometry")
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 1)}
        (ordinary-shared/add-humetty-and-voided context))))

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        variant (interface/get-sanitized-data (c/++ context :variant))
        origin (interface/get-sanitized-data (c/++ context :origin))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
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
        [shape environment-points] (get parts target-part-index)
        shape (ordinary-shared/adjust-shape shape width (-> width (/ 2) (* size) (/ 100)) context)
        part [shape environment-points]]
    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary-shared/adjusted-shape-outline
      shape outline? context
      [line/render line [line-one-data line-two-data] (case target-part-index
                                                        0 point-top
                                                        1 point-top
                                                        2 point-bottom
                                                        3 point-bottom) outline? context])
     [cottising/render-chevron-cottise
      (c/++ context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :distance-fn (fn [distance half-joint-angle-rad]
                     (-> (- distance)
                         (/ 100)
                         (* size)
                         (+ line-top-min)
                         (/ (if (zero? half-joint-angle-rad)
                              0.00001
                              (Math/sin half-joint-angle-rad)))))
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
