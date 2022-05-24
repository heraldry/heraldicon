(ns heraldicon.heraldry.ordinary.type.quarter
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/quarter)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/quarter)

(def variant-choices
  [[:string.option.variant-quarter-choice/dexter-chief :dexter-chief]
   [:string.option.variant-quarter-choice/sinister-chief :sinister-chief]
   [:string.option.variant-quarter-choice/dexter-base :dexter-base]
   [:string.option.variant-quarter-choice/sinister-base :sinister-base]])

(def variant-map
  (options/choices->map variant-choices))

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :choice
                       :choices [[:string.option.point-choice/fess :fess]
                                 [:string.option.point-choice/chief :chief]
                                 [:string.option.point-choice/base :base]
                                 [:string.option.point-choice/honour :honour]
                                 [:string.option.point-choice/nombril :nombril]
                                 [:string.option.point-choice/top :top]
                                 [:string.option.point-choice/bottom :bottom]]
                       :default :fess
                       :ui {:label :string.option/point}}
               :alignment {:type :choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui {:label :string.option/alignment
                                :form-type :radio-select}}
               :offset-y {:type :range
                          :min -45
                          :max 45
                          :default 0
                          :ui {:label :string.option/offset-y
                               :step 0.1}}
               :ui {:label :string.option/anchor
                    :form-type :position}}
      :line line-style
      :opposite-line opposite-line-style
      :variant {:type :choice
                :choices variant-choices
                :default :dexter-chief
                :ui {:label :string.option/variant
                     :form-type :select}}
      :geometry {:size {:type :range
                        :min 10
                        :max 150
                        :default 100
                        :ui {:label :string.option/size
                             :step 0.1}}
                 :ui {:label :string.option/geometry
                      :form-type :geometry}}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        variant (interface/get-sanitized-data (c/++ context :variant))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        points (:points environment)
        width (:width environment)
        height (:height environment)
        anchor-point (position/calculate anchor environment :fess)
        top (assoc (:top points) :x (:x anchor-point))
        top-left (:top-left points)
        top-right (:top-right points)
        bottom (assoc (:bottom points) :x (:x anchor-point))
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y anchor-point))
        right (assoc (:right points) :y (:y anchor-point))
        target-part-index (get {:dexter-chief 0
                                :sinister-chief 1
                                :dexter-base 2
                                :sinister-base 3} variant 0)
        relevant-corner (case target-part-index
                          0 top-left
                          1 top-right
                          2 bottom-left
                          3 bottom-right)
        anchor-point (-> anchor-point
                         (v/sub relevant-corner)
                         (v/mul (/ size 100))
                         (v/add relevant-corner))

        intersection-top (v/find-first-intersection-of-ray anchor-point top environment)
        intersection-bottom (v/find-first-intersection-of-ray anchor-point bottom environment)
        intersection-left (v/find-first-intersection-of-ray anchor-point left environment)
        intersection-right (v/find-first-intersection-of-ray anchor-point right environment)
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
                                  (v/sub anchor-point)
                                  v/abs))
                        (apply max))
        full-arm-length (+ arm-length 30)
        point-top (-> (v/Vector. 0 -1)
                      (v/mul full-arm-length)
                      (v/add anchor-point))
        point-bottom (-> (v/Vector. 0 1)
                         (v/mul full-arm-length)
                         (v/add anchor-point))
        point-left (-> (v/Vector. -1 0)
                       (v/mul full-arm-length)
                       (v/add anchor-point))
        point-right (-> (v/Vector. 1 0)
                        (v/mul full-arm-length)
                        (v/add anchor-point))
        {line-top :line
         line-top-start :line-start
         :as line-top-data
         line-top-min :line-min} (line/create line
                                              anchor-point point-top
                                              :reversed? true
                                              :real-start 0
                                              :real-end arm-length
                                              :context context
                                              :environment environment)
        {line-right :line
         line-right-start :line-start
         :as line-right-data} (line/create opposite-line
                                           anchor-point point-right
                                           :flipped? true
                                           :mirrored? true
                                           :real-start 0
                                           :real-end arm-length
                                           :context context
                                           :environment environment)
        {line-bottom :line
         line-bottom-start :line-start
         :as line-bottom-data} (line/create line
                                            anchor-point point-bottom
                                            :reversed? true
                                            :real-start 0
                                            :real-end arm-length
                                            :context context
                                            :environment environment)
        {line-left :line
         line-left-start :line-start
         :as line-left-data} (line/create opposite-line
                                          anchor-point point-left
                                          :flipped? true
                                          :mirrored? true
                                          :real-start 0
                                          :real-end arm-length
                                          :context context
                                          :environment environment)
        parts [[["M" (v/add point-top
                            line-top-start)
                 (path/stitch line-top)
                 "L" anchor-point
                 (path/stitch line-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [(v/add point-left
                                        line-left-start)
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [top-left anchor-point]]

               [["M" (v/add point-top
                            line-top-start)
                 (path/stitch line-top)
                 "L" anchor-point
                 (path/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [(v/add point-right
                                        line-right-start)
                                 (v/add point-top
                                        line-top-start)])
                 "z"]
                [anchor-point top-right]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" anchor-point
                 (path/stitch line-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [(v/add point-left
                                        line-left-start)
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [anchor-point bottom-left]]

               [["M" (v/add point-bottom
                            line-bottom-start)
                 (path/stitch line-bottom)
                 "L" anchor-point
                 (path/stitch line-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [(v/add point-right
                                        line-right-start)
                                 (v/add point-bottom
                                        line-bottom-start)])
                 "z"]
                [anchor-point bottom-right]]]
        [line-one-data
         line-two-data] (case target-part-index
                          0 [line-top-data line-left-data]
                          1 [line-top-data line-right-data]
                          2 [line-bottom-data line-left-data]
                          3 [line-bottom-data line-right-data])
        [shape environment-points] (get parts target-part-index)
        shape (ordinary.shared/adjust-shape shape width (-> width (/ 2) (* size) (/ 100)) context)
        part [shape environment-points]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
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
      :corner-point anchor-point]]))
