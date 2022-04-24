(ns heraldry.coat-of-arms.ordinary.type.chevron
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field.shared]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary.interface]
   [heraldry.coat-of-arms.ordinary.shared :as ordinary.shared]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.chevron :as chevron]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.core :as math]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/chevron)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/chevron)

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        origin-point-option {:type :choice
                             :choices [[:string.option.point-choice/chief :chief]
                                       [:string.option.point-choice/base :base]
                                       [:string.option.point-choice/dexter :dexter]
                                       [:string.option.point-choice/sinister :sinister]
                                       [:string.option.point-choice/top-left :top-left]
                                       [:string.option.point-choice/top-right :top-right]
                                       [:string.option.point-choice/bottom-left :bottom-left]
                                       [:string.option.point-choice/bottom-right :bottom-right]
                                       [:string.option.orientation-point-choice/angle :angle]]
                             :default :base
                             :ui {:label :string.option/point}}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        orientation-point-option {:type :choice
                                  :choices (case current-origin-point
                                             :base [[:string.option.point-choice/bottom-left :bottom-left]
                                                    [:string.option.point-choice/bottom-right :bottom-right]
                                                    [:string.option.point-choice/left :left]
                                                    [:string.option.point-choice/right :right]
                                                    [:string.option.orientation-point-choice/angle :angle]]
                                             :chief [[:string.option.point-choice/top-left :top-left]
                                                     [:string.option.point-choice/top-right :top-right]
                                                     [:string.option.point-choice/left :left]
                                                     [:string.option.point-choice/right :right]
                                                     [:string.option.orientation-point-choice/angle :angle]]
                                             :dexter [[:string.option.point-choice/top-left :top-left]
                                                      [:string.option.point-choice/bottom-left :bottom-left]
                                                      [:string.option.point-choice/top :top]
                                                      [:string.option.point-choice/bottom :bottom]
                                                      [:string.option.orientation-point-choice/angle :angle]]
                                             :sinister [[:string.option.point-choice/top-right :top-right]
                                                        [:string.option.point-choice/bottom-right :bottom-right]
                                                        [:string.option.point-choice/top :top]
                                                        [:string.option.point-choice/bottom :bottom]
                                                        [:string.option.orientation-point-choice/angle :angle]]
                                             :bottom-left [[:string.option.point-choice/bottom :bottom]
                                                           [:string.option.point-choice/bottom-right :bottom-right]
                                                           [:string.option.point-choice/top-left :top-left]
                                                           [:string.option.point-choice/left :left]
                                                           [:string.option.orientation-point-choice/angle :angle]]
                                             :bottom-right [[:string.option.point-choice/bottom-left :bottom-left]
                                                            [:string.option.point-choice/bottom :bottom]
                                                            [:string.option.point-choice/right :right]
                                                            [:string.option.point-choice/top-right :top-right]
                                                            [:string.option.orientation-point-choice/angle :angle]]
                                             :top-left [[:string.option.point-choice/top :top]
                                                        [:string.option.point-choice/top-right :top-right]
                                                        [:string.option.point-choice/left :left]
                                                        [:string.option.point-choice/bottom-left :bottom-left]
                                                        [:string.option.orientation-point-choice/angle :angle]]
                                             :top-right [[:string.option.point-choice/top-left :top-left]
                                                         [:string.option.point-choice/top :top]
                                                         [:string.option.point-choice/left :right]
                                                         [:string.option.point-choice/bottom-right :bottom-right]
                                                         [:string.option.orientation-point-choice/angle :angle]]
                                             [[:string.option.point-choice/top-left :top-left]
                                              [:string.option.point-choice/top :top]
                                              [:string.option.point-choice/top-right :top-right]
                                              [:string.option.point-choice/left :left]
                                              [:string.option.point-choice/right :right]
                                              [:string.option.point-choice/bottom-left :bottom-left]
                                              [:string.option.point-choice/bottom :bottom]
                                              [:string.option.point-choice/bottom-right :bottom-right]
                                              [:string.option.orientation-point-choice/angle :angle]])
                                  :default (case current-origin-point
                                             :base :bottom-left
                                             :chief :top-right
                                             :dexter :top-left
                                             :sinister :bottom-right
                                             :bottom-left :left
                                             :bottom-right :right
                                             :top-left :left
                                             :top-right :right
                                             :angle :angle
                                             :bottom-left)
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (-> {:anchor {:point {:type :choice
                          :choices [[:string.option.point-choice/fess :fess]
                                    [:string.option.point-choice/chief :chief]
                                    [:string.option.point-choice/base :base]
                                    [:string.option.point-choice/honour :honour]
                                    [:string.option.point-choice/nombril :nombril]
                                    [:string.option.point-choice/top-left :top-left]
                                    [:string.option.point-choice/top :top]
                                    [:string.option.point-choice/top-right :top-right]
                                    [:string.option.point-choice/left :left]
                                    [:string.option.point-choice/right :right]
                                    [:string.option.point-choice/bottom-left :bottom-left]
                                    [:string.option.point-choice/bottom :bottom]
                                    [:string.option.point-choice/bottom-right :bottom-right]]
                          :default :fess
                          :ui {:label :string.option/point}}
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
         :origin (cond-> {:point origin-point-option
                          :ui {:label :string.charge.attitude/issuant
                               :form-type :position}}

                   (= current-origin-point
                      :angle) (assoc :angle {:type :range
                                             :min -180
                                             :max 180
                                             :default 0
                                             :ui {:label :string.option/angle}})

                   (not= current-origin-point
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
                                                        :step 0.1}}))
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
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        origin (interface/get-sanitized-data (c/++ context :origin))
        origin (update origin :point (fn [origin-point]
                                       (get {:chief :top
                                             :base :bottom
                                             :dexter :left
                                             :sinister :right} origin-point origin-point)))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        raw-origin (interface/get-raw-data (c/++ context :origin))
        origin (cond-> origin
                 (-> origin
                     :point
                     #{:left
                       :right
                       :top
                       :bottom}) (->
                                   (assoc :offset-x (or (:offset-x raw-origin)
                                                        (:offset-x anchor)))
                                   (assoc :offset-y (or (:offset-y raw-origin)
                                                        (:offset-y anchor)))))
        points (:points environment)
        unadjusted-anchor-point (position/calculate anchor environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of height)))
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (angle/calculate-anchor-and-orientation
                                          environment
                                          anchor
                                          origin
                                          0
                                          90)
        chevron-angle (math/normalize-angle
                       (v/angle-to-point direction-anchor-point
                                         origin-point))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (angle/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               band-width
                                               chevron-angle)
        [mirrored-anchor mirrored-orientation] [(chevron/mirror-point chevron-angle unadjusted-anchor-point anchor-point)
                                                (chevron/mirror-point chevron-angle unadjusted-anchor-point orientation-point)]
        anchor-point (v/line-intersection anchor-point orientation-point
                                          mirrored-anchor mirrored-orientation)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle anchor-point orientation-point)
        diagonal-left (v/add anchor-point relative-left)
        diagonal-right (v/add anchor-point relative-right)
        angle-left (math/normalize-angle (v/angle-to-point anchor-point diagonal-left))
        angle-right (math/normalize-angle (v/angle-to-point anchor-point diagonal-right))
        joint-angle (math/normalize-angle (- angle-left angle-right))
        delta (/ band-width 2 (Math/sin (-> joint-angle
                                            (* Math/PI)
                                            (/ 180)
                                            (/ 2))))
        offset-lower (v/rotate
                      (v/v delta 0)
                      chevron-angle)
        offset-upper (v/rotate
                      (v/v (- delta) 0)
                      chevron-angle)
        corner-upper (v/add anchor-point offset-upper)
        corner-lower (v/add anchor-point offset-lower)
        left-upper (v/add diagonal-left offset-upper)
        left-lower (v/add diagonal-left offset-lower)
        right-upper (v/add diagonal-right offset-upper)
        right-lower (v/add diagonal-right offset-lower)
        intersection-left-upper (v/find-first-intersection-of-ray corner-upper left-upper environment)
        intersection-right-upper (v/find-first-intersection-of-ray corner-upper right-upper environment)
        intersection-left-lower (v/find-first-intersection-of-ray corner-lower left-lower environment)
        intersection-right-lower (v/find-first-intersection-of-ray corner-lower right-lower environment)
        end-left-upper (-> intersection-left-upper
                           (v/sub corner-upper)
                           v/abs)
        end-right-upper (-> intersection-right-upper
                            (v/sub corner-upper)
                            v/abs)
        end-left-lower (-> intersection-left-lower
                           (v/sub corner-lower)
                           v/abs)
        end-right-lower (-> intersection-right-lower
                            (v/sub corner-lower)
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
                                                 :context context
                                                 :environment environment)
        {line-right-lower :line
         line-right-lower-start :line-start
         line-right-lower-min :line-min
         :as line-right-lower-data} (line/create opposite-line
                                                 corner-lower right-lower
                                                 :reversed? true
                                                 :real-start 0
                                                 :real-end end
                                                 :context context
                                                 :environment environment)
        {line-left-lower :line
         line-left-lower-start :line-start
         :as line-left-lower-data} (line/create opposite-line
                                                corner-lower left-lower
                                                :real-start 0
                                                :real-end end
                                                :context context
                                                :environment environment)
        {line-left-upper :line
         line-left-upper-start :line-start
         :as line-left-upper-data} (line/create line
                                                corner-upper left-upper
                                                :reversed? true
                                                :real-start 0
                                                :real-end end
                                                :context context
                                                :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add corner-upper
                           line-right-upper-start)
                (path/stitch line-right-upper)
                "L" (v/add right-lower
                           line-right-lower-start)
                (path/stitch line-right-lower)
                "L" (v/add corner-lower
                           line-left-lower-start)
                (path/stitch line-left-lower)
                "L" (v/add left-upper
                           line-left-upper-start)
                (path/stitch line-left-upper)
                "z"]
               width
               band-width
               context)
        part [shape
              [top-left bottom-right]]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [:<>
       [line/render line [line-left-upper-data
                          line-right-upper-data] left-upper outline? context]
       [line/render opposite-line [line-right-lower-data
                                   line-left-lower-data] right-lower outline? context]])
     [cottising/render-chevron-cottise
      (c/++ context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :distance-fn (fn [distance half-joint-angle-rad]
                     (-> (- distance)
                         (/ 100)
                         (* height)
                         (+ line-right-upper-min)
                         (/ (if (zero? half-joint-angle-rad)
                              0.00001
                              (Math/sin half-joint-angle-rad)))))
      :alignment :left
      :width width
      :height height
      :chevron-angle chevron-angle
      :joint-angle joint-angle
      :corner-point corner-upper]
     [cottising/render-chevron-cottise
      (c/++ context :cottising :cottise-opposite-1)
      :cottise-opposite-2 :cottise-opposite-1
      :distance-fn (fn [distance half-joint-angle-rad]
                     (-> (+ distance)
                         (/ 100)
                         (* height)
                         (- line-right-lower-min)
                         (/ (if (zero? half-joint-angle-rad)
                              0.00001
                              (Math/sin half-joint-angle-rad)))))
      :alignment :right
      :width width
      :height height
      :chevron-angle chevron-angle
      :joint-angle joint-angle
      :corner-point corner-lower
      :swap-lines? true]]))
