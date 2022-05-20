(ns heraldicon.heraldry.ordinary.type.pile
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.heraldry.shared.pile :as pile]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/pile)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/pile)

(def size-mode-choices
  [[:string.option.size-mode-choice/thickness :thickness]
   [:string.option.size-mode-choice/angle :angle]])

(def size-mode-map
  (options/choices->map size-mode-choices))

(def orientation-type-choices
  [[:string.option.orientation-type-choice/edge :edge]
   [:string.option.orientation-type-choice/orientation-point :point]])

(def orientation-type-map
  (options/choices->map orientation-type-choices))

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        anchor-point-option {:type :choice
                             :choices [[:string.option.point-choice/chief :chief]
                                       [:string.option.point-choice/base :base]
                                       [:string.option.point-choice/dexter :dexter]
                                       [:string.option.point-choice/sinister :sinister]
                                       [:string.option.point-choice/top-left :top-left]
                                       [:string.option.point-choice/top :top]
                                       [:string.option.point-choice/top-right :top-right]
                                       [:string.option.point-choice/left :left]
                                       [:string.option.point-choice/right :right]
                                       [:string.option.point-choice/bottom-left :bottom-left]
                                       [:string.option.point-choice/bottom :bottom]
                                       [:string.option.point-choice/bottom-right :bottom-right]]
                             :default :top
                             :ui {:label :string.option/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :choice
                                  :choices (options/filter-choices
                                            [[:string.option.point-choice/top-left :top-left]
                                             [:string.option.point-choice/top :top]
                                             [:string.option.point-choice/top-right :top-right]
                                             [:string.option.point-choice/left :left]
                                             [:string.option.point-choice/right :right]
                                             [:string.option.point-choice/bottom-left :bottom-left]
                                             [:string.option.point-choice/bottom :bottom]
                                             [:string.option.point-choice/bottom-right :bottom-right]
                                             [:string.option.point-choice/fess :fess]
                                             [:string.option.point-choice/chief :chief]
                                             [:string.option.point-choice/base :base]
                                             [:string.option.point-choice/dexter :dexter]
                                             [:string.option.point-choice/sinister :sinister]
                                             [:string.option.point-choice/honour :honour]
                                             [:string.option.point-choice/nombril :nombril]
                                             [:string.option.orientation-point-choice/angle :angle]]
                                            #(not= % current-anchor-point))
                                  :default :fess
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)
        size-mode-option {:type :choice
                          :choices size-mode-choices
                          :default :thickness
                          :ui {:label :string.option/size-mode
                               :form-type :radio-select}}
        current-size-mode (options/get-value
                           (interface/get-raw-data (c/++ context :geometry :size-mode))
                           size-mode-option)]
    (-> {:anchor {:point anchor-point-option
                  :alignment {:type :choice
                              :choices position/alignment-choices
                              :default :middle
                              :ui {:label :string.option/alignment
                                   :form-type :radio-select}}
                  :offset-x {:type :range
                             :min -50
                             :max 50
                             :default 0
                             :ui {:label :string.option/offset-x
                                  :step 0.1}}
                  :offset-y {:type :range
                             :min -75
                             :max 75
                             :default 0
                             :ui {:label :string.option/offset-y
                                  :step 0.1}}
                  :ui {:label :string.option/anchor
                       :form-type :position}}
         :orientation (cond-> {:point orientation-point-option
                               :ui {:label :string.option/orientation
                                    :form-type :position}}

                        (= current-orientation-point
                           :angle) (assoc :angle {:type :range
                                                  :min (cond
                                                         (#{:top-left
                                                            :top-right
                                                            :bottom-left
                                                            :bottom-right} current-anchor-point) 0
                                                         :else -90)
                                                  :max 90
                                                  :default (cond
                                                             (#{:top-left
                                                                :top-right
                                                                :bottom-left
                                                                :bottom-right} current-anchor-point) 45
                                                             :else 0)
                                                  :ui {:label :string.option/angle}})

                        (not= current-orientation-point
                              :angle) (assoc :offset-x {:type :range
                                                        :min -50
                                                        :max 50
                                                        :default 0
                                                        :ui {:label :string.option/offset-x
                                                             :step 0.1}}
                                             :offset-y {:type :range
                                                        :min -75
                                                        :max 75
                                                        :default 0
                                                        :ui {:label :string.option/offset-y
                                                             :step 0.1}}
                                             :type {:type :choice
                                                    :choices orientation-type-choices
                                                    :default :edge
                                                    :ui {:label :string.render-options/mode
                                                         :form-type :radio-select}}))
         :line line-style
         :opposite-line opposite-line-style
         :geometry {:size-mode size-mode-option
                    :size {:type :range
                           :min 5
                           :max 120
                           :default (case current-size-mode
                                      :thickness 75
                                      30)
                           :ui {:label :string.option/size
                                :step 0.1}}
                    :stretch {:type :range
                              :min 0.33
                              :max 2
                              :default 0.85
                              :ui {:label :string.option/stretch
                                   :step 0.01}}
                    :ui {:label :string.option/geometry
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 1)}
        (ordinary.shared/add-humetty-and-voided context))))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        geometry (interface/get-sanitized-data (c/++ context :geometry))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        width (:width environment)
        height (:height environment)
        thickness-base (if (#{:left :right} (:point anchor))
                         height
                         width)
        {anchor-point :anchor
         point :point
         thickness :thickness} (pile/calculate-properties
                                environment
                                anchor
                                (cond-> orientation
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point anchor)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                thickness-base
                                (case (:point anchor)
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        pile-angle (v/angle-to-point point anchor-point)
        {left-point :left
         right-point :right} (pile/diagonals anchor-point point thickness)
        intersection-left (-> (v/environment-intersections point left-point environment)
                              last)
        intersection-right (-> (v/environment-intersections point right-point environment)
                               last)
        joint-angle (v/angle-between-vectors (v/sub intersection-left point)
                                             (v/sub intersection-right point))
        end-left (-> intersection-left
                     (v/sub point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (math/percent-of thickness-base))
                 (update-in [:fimbriation :thickness-2] (math/percent-of thickness-base)))
        {line-left :line
         line-left-start :line-start
         line-left-min :line-min
         :as line-left-data} (line/create line
                                          point left-point
                                          :reversed? true
                                          :real-start 0
                                          :real-end end
                                          :context context
                                          :environment environment)
        {line-right :line
         :as line-right-data} (line/create opposite-line
                                           point right-point
                                           :real-start 0
                                           :real-end end
                                           :context context
                                           :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add left-point
                           line-left-start)
                (path/stitch line-left)
                (path/stitch line-right)
                "z"]
               width
               thickness
               context)
        part [shape
              [top-left top-right
               bottom-left bottom-right]]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [line/render line [line-left-data
                         line-right-data] left-point outline? context])
     [cottising/render-chevron-cottise
      (c/++ context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :distance-fn (fn [distance half-joint-angle-rad]
                     (-> (- distance)
                         (/ 100)
                         (* thickness-base)
                         (+ line-left-min)
                         (/ (if (zero? half-joint-angle-rad)
                              0.00001
                              (Math/sin half-joint-angle-rad)))))
      :alignment :left
      :width width
      :height height
      :chevron-angle pile-angle
      :joint-angle joint-angle
      :corner-point point]]))
