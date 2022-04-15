(ns heraldry.coat-of-arms.field.type.per-chevron
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field.interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.chevron :as chevron]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.core :as math]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]))

(def field-type :heraldry.field.type/per-chevron)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-chevron)

(defmethod field.interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
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
    {:anchor {:point {:type :choice
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
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label :string.option/offset-x
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
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
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label :string.option/offset-x
                                                    :step 0.1}}
                                    :offset-y {:type :range
                                               :min -45
                                               :max 45
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
                          :angle) (assoc :offset-x {:type :range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui {:label :string.option/offset-x
                                                         :step 0.1}}
                                         :offset-y {:type :range
                                                    :min -45
                                                    :max 45
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
                     :form-type :geometry}}}))

(defmethod field.interface/render-field field-type
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
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
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
                                               0
                                               chevron-angle)
        [mirrored-anchor mirrored-orientation] [(chevron/mirror-point chevron-angle unadjusted-anchor-point anchor-point)
                                                (chevron/mirror-point chevron-angle unadjusted-anchor-point orientation-point)]
        anchor-point (v/line-intersection anchor-point orientation-point
                                          mirrored-anchor mirrored-orientation)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle anchor-point orientation-point)
        diagonal-left (v/add anchor-point relative-left)
        diagonal-right (v/add anchor-point relative-right)
        intersection-left (v/find-first-intersection-of-ray anchor-point diagonal-left environment)
        intersection-right (v/find-first-intersection-of-ray anchor-point diagonal-right environment)
        end-left (-> intersection-left
                     (v/sub anchor-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub anchor-point)
                      v/abs)
        end (max end-left end-right)
        {line-left :line
         line-left-start :line-start
         :as line-left-data} (line/create line
                                          anchor-point diagonal-left
                                          :real-start 0
                                          :real-end end
                                          :reversed? true
                                          :context context
                                          :environment environment)
        {line-right :line
         line-right-end :line-end
         :as line-right-data} (line/create opposite-line
                                           anchor-point diagonal-right
                                           :real-start 0
                                           :real-end end
                                           :context context
                                           :environment environment)
        infinity-points (cond
                          (<= 45 chevron-angle 135) [:right :left]
                          (<= 135 chevron-angle 225) [:bottom :top]
                          (<= 225 chevron-angle 315) [:left :right]
                          :else [:top :bottom])
        parts [[["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 (path/stitch line-right)
                 (infinity/path :counter-clockwise
                                infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 (path/stitch line-right)
                 (infinity/path :clockwise
                                infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil]
      environment]
     [line/render line [line-left-data
                        line-right-data] diagonal-left outline? context]]))
