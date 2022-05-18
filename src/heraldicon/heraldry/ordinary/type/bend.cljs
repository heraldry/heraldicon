(ns heraldicon.heraldry.ordinary.type.bend
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/bend)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bend)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        anchor-point-option {:type :choice
                             :choices [[:string.option.point-choice/fess :fess]
                                       [:string.option.point-choice/chief :chief]
                                       [:string.option.point-choice/base :base]
                                       [:string.option.point-choice/honour :honour]
                                       [:string.option.point-choice/nombril :nombril]
                                       [:string.option.point-choice/top-left :top-left]
                                       [:string.option.point-choice/bottom-right :bottom-right]]
                             :default :top-left
                             :ui {:label :string.option/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :choice
                                  :choices (case current-anchor-point
                                             :top-left [[:string.option.point-choice/fess :fess]
                                                        [:string.option.point-choice/chief :chief]
                                                        [:string.option.point-choice/base :base]
                                                        [:string.option.point-choice/honour :honour]
                                                        [:string.option.point-choice/nombril :nombril]
                                                        [:string.option.point-choice/bottom-right :bottom-right]
                                                        [:string.option.orientation-point-choice/angle :angle]]
                                             :bottom-right [[:string.option.point-choice/fess :fess]
                                                            [:string.option.point-choice/chief :chief]
                                                            [:string.option.point-choice/base :base]
                                                            [:string.option.point-choice/honour :honour]
                                                            [:string.option.point-choice/nombril :nombril]
                                                            [:string.option.point-choice/top-left :top-left]
                                                            [:string.option.orientation-point-choice/angle :angle]]
                                             [[:string.option.point-choice/top-left :top-left]
                                              [:string.option.point-choice/bottom-right :bottom-right]
                                              [:string.option.orientation-point-choice/angle :angle]])
                                  :default (case current-anchor-point
                                             :top-left :fess
                                             :bottom-right :fess
                                             :top-left)
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (-> {:anchor {:point anchor-point-option
                  :alignment {:type :choice
                              :choices position/alignment-choices
                              :default :middle
                              :ui {:label :string.option/alignment
                                   :form-type :radio-select}}
                  :offset-x {:type :range
                             :min -75
                             :max 75
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
                                                  :min 0
                                                  :max 360
                                                  :default 45
                                                  :ui {:label :string.option/angle}})

                        (not= current-orientation-point
                              :angle) (assoc :alignment {:type :choice
                                                         :choices position/alignment-choices
                                                         :default :middle
                                                         :ui {:label :string.option/alignment
                                                              :form-type :radio-select}}
                                             :offset-x {:type :range
                                                        :min -75
                                                        :max 75
                                                        :default 0
                                                        :ui {:label :string.option/offset-x
                                                             :step 0.1}}
                                             :offset-y {:type :range
                                                        :min -75
                                                        :max 75
                                                        :default 0
                                                        :ui {:label :string.option/offset-y
                                                             :step 0.1}}))
         :line line-style
         :opposite-line opposite-line-style
         :geometry {:size {:type :range
                           :min 0.1
                           :max 90
                           :default 25
                           :ui {:label :string.option/size
                                :step 0.1}}
                    :ui {:label :string.option/geometry
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 2)}
        (ordinary.shared/add-humetty-and-voided context))))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment
           override-middle-real-start
           override-middle-real-end
           override-real-start
           override-real-end
           override-center-point] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top (:top points)
        bottom (:bottom points)
        width (:width environment)
        height (:height environment)
        band-height (-> size
                        ((math/percent-of height)))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               band-height
                                               nil)
        center-point (or override-center-point
                         (v/line-intersection anchor-point orientation-point
                                              top bottom))
        direction (v/sub orientation-point anchor-point)
        direction (-> (v/Vector. (-> direction :x Math/abs)
                                 (-> direction :y Math/abs))
                      v/normal)
        direction-orthogonal (v/orthogonal direction)
        [middle-real-start
         middle-real-end] (if (and override-middle-real-start
                                   override-middle-real-end)
                            [override-middle-real-start
                             override-middle-real-end]
                            (v/environment-intersections
                             anchor-point
                             (v/add anchor-point direction)
                             environment))
        band-length (-> (v/sub middle-real-start center-point)
                        v/abs
                        (* 2))
        middle-start (-> direction
                         (v/mul -30)
                         (v/add middle-real-start))
        middle-end (-> direction
                       (v/mul 30)
                       (v/add middle-real-end))
        width-offset (-> direction-orthogonal
                         (v/mul band-height)
                         (v/div 2))
        ordinary-top-left (v/add middle-real-start width-offset)
        first-start (v/add middle-start width-offset)
        first-end (v/add middle-end width-offset)
        second-start (v/sub middle-start width-offset)
        second-end (v/sub middle-end width-offset)
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
                       (min (-> (v/sub first-real-start first-start)
                                (v/abs))
                            (-> (v/sub second-real-start second-start)
                                (v/abs))))
        real-end (or override-real-end
                     (max (-> (v/sub first-real-start first-start)
                              (v/abs))
                          (-> (v/sub second-real-end second-start)
                              (v/abs))))
        angle (v/angle-to-point middle-start middle-end)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (math/percent-of height))
                 (update-in [:fimbriation :thickness-2] (math/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (math/percent-of height))
                          (update-in [:fimbriation :thickness-2] (math/percent-of height)))
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
        counterchanged? (= (interface/get-sanitized-data (c/++ context :field :type))
                           :heraldry.field.type/counterchanged)
        inherit-environment? (interface/get-sanitized-data (c/++ context :field :inherit-environment?))
        use-parent-environment? (or counterchanged?
                                    inherit-environment?)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add first-start
                           line-one-start)
                (path/stitch line-one)
                "L" (v/add second-end
                           line-reversed-start)
                (path/stitch line-reversed)
                "L" (v/add first-start
                           line-one-start)
                "z"]
               width
               band-height
               context)
        part [shape
              (if use-parent-environment?
                [first-real-start first-real-end
                 second-real-start second-real-end]
                [v/zero
                 (v/Vector. band-length band-height)])]
        cottise-context (merge
                         context
                         {:override-real-start real-start
                          :override-real-end real-end})]
    [:<>
     [field.shared/make-subfield
      (-> context
          (c/++ :field)
          (c/<< :transform (when (not use-parent-environment?)
                             (str "translate(" (v/->str ordinary-top-left) ")"
                                  "rotate(" angle ")"))))
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [:<>
       [line/render line [line-one-data] first-start outline? context]
       [line/render opposite-line [line-reversed-data] second-end outline? context]])
     [cottising/render-bend-cottise
      (c/++ cottise-context :cottising :cottise-1)
      :cottise-2 :cottise-1
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
      :angle angle
      :direction-orthogonal direction-orthogonal
      :center-point center-point
      :middle-real-start-fn (fn [point-offset]
                              (v/add middle-real-start point-offset))
      :middle-real-end-fn (fn [point-offset]
                            (v/add middle-real-end point-offset))]
     [cottising/render-bend-cottise
      (c/++ cottise-context :cottising :cottise-opposite-1)
      :cottise-opposite-2 :cottise-opposite-1
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
      :angle angle
      :direction-orthogonal direction-orthogonal
      :center-point center-point
      :middle-real-start-fn (fn [point-offset]
                              (v/sub middle-real-start point-offset))
      :middle-real-end-fn (fn [point-offset]
                            (v/sub middle-real-end point-offset))
      :swap-lines? true]]))
