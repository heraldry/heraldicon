(ns heraldry.coat-of-arms.field.type.tierced-per-pall
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field.interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.chevron :as chevron]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.core :as math]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]))

(def field-type :heraldry.field.type/tierced-per-pall)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-pall)

(defmethod field.interface/part-names field-type [_] ["middle" "side I" "side II"])

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        extra-line-style (-> (line/options (c/++ context :extra-line)
                                           :fimbriation? false
                                           :inherited-options line-style)
                             (options/override-if-exists [:offset :min] 0)
                             (options/override-if-exists [:base-line] nil))
        direction-intermediate-name-point-option {:type :choice
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
                                                            [:string.option.point-choice/bottom-right :bottom-right]
                                                            [:string.option.orientation-point-choice/angle :angle]]
                                                  :default :top
                                                  :ui {:label :string.option/point}}
        current-direction-intermediate-name-point (options/get-value
                                                   (interface/get-raw-data (c/++ context :direction-intermediate-name :point))
                                                   direction-intermediate-name-point-option)
        orientation-point-option {:type :choice
                                  :choices (case current-direction-intermediate-name-point
                                             :bottom [[:string.option.point-choice/bottom-left :bottom-left]
                                                      [:string.option.point-choice/bottom :bottom]
                                                      [:string.option.point-choice/bottom-right :bottom-right]
                                                      [:string.option.point-choice/left :left]
                                                      [:string.option.point-choice/right :right]
                                                      [:string.option.orientation-point-choice/angle :angle]]
                                             :top [[:string.option.point-choice/top-left :top-left]
                                                   [:string.option.point-choice/top :top]
                                                   [:string.option.point-choice/top-right :top-right]
                                                   [:string.option.point-choice/left :left]
                                                   [:string.option.point-choice/right :right]
                                                   [:string.option.orientation-point-choice/angle :angle]]
                                             :left [[:string.option.point-choice/top-left :top-left]
                                                    [:string.option.point-choice/left :left]
                                                    [:string.option.point-choice/bottom-left :bottom-left]
                                                    [:string.option.point-choice/top :top]
                                                    [:string.option.point-choice/bottom :bottom]
                                                    [:string.option.orientation-point-choice/angle :angle]]
                                             :right [[:string.option.point-choice/top-right :top-right]
                                                     [:string.option.point-choice/right :right]
                                                     [:string.option.point-choice/bottom-right :bottom-right]
                                                     [:string.option.point-choice/top :top]
                                                     [:string.option.point-choice/bottom :bottom]
                                                     [:string.option.orientation-point-choice/angle :angle]]
                                             :bottom-left [[:string.option.point-choice/bottom-left :bottom-left]
                                                           [:string.option.point-choice/bottom :bottom]
                                                           [:string.option.point-choice/bottom-right :bottom-right]
                                                           [:string.option.point-choice/top-left :top-left]
                                                           [:string.option.point-choice/left :left]
                                                           [:string.option.orientation-point-choice/angle :angle]]
                                             :bottom-right [[:string.option.point-choice/bottom-left :bottom-left]
                                                            [:string.option.point-choice/bottom :bottom]
                                                            [:string.option.point-choice/bottom-right :bottom-right]
                                                            [:string.option.point-choice/right :right]
                                                            [:string.option.point-choice/top-right :top-right]
                                                            [:string.option.orientation-point-choice/angle :angle]]
                                             :top-left [[:string.option.point-choice/top-left :top-left]
                                                        [:string.option.point-choice/top :top]
                                                        [:string.option.point-choice/top-right :top-right]
                                                        [:string.option.point-choice/left :left]
                                                        [:string.option.point-choice/bottom-left :bottom-left]
                                                        [:string.option.orientation-point-choice/angle :angle]]
                                             :top-right [[:string.option.point-choice/top-left :top-left]
                                                         [:string.option.point-choice/top :top]
                                                         [:string.option.point-choice/top-right :top-right]
                                                         [:string.option.point-choice/left :left]
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
                                  :default (case current-direction-intermediate-name-point
                                             :bottom :bottom-left
                                             :top :top-right
                                             :left :top-left
                                             :right :bottom-right
                                             :bottom-left :left
                                             :bottom-right :bottom
                                             :top-left :top
                                             :top-right :right
                                             :angle :angle
                                             :bottom-left)
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:origin {:point {:type :choice
                      :choices [[:string.option.point-choice/chief :chief]
                                [:string.option.point-choice/base :base]
                                [:string.option.point-choice/fess :fess]
                                [:string.option.point-choice/dexter :dexter]
                                [:string.option.point-choice/sinister :sinister]
                                [:string.option.point-choice/honour :honour]
                                [:string.option.point-choice/nombril :nombril]
                                [:string.option.point-choice/top-left :top-left]
                                [:string.option.point-choice/top :top]
                                [:string.option.point-choice/top-right :top-right]
                                [:string.option.point-choice/left :left]
                                [:string.option.point-choice/right :right]
                                [:string.option.point-choice/bottom-left :bottom-left]
                                [:string.option.point-choice/bottom :bottom]
                                [:string.option.point-choice/bottom-right :bottom-right]
                                [:string.option.orientation-point-choice/angle :angle]]
                      :default :fess
                      :ui {:label :string.option/point}}
              :alignment {:type :choice
                          :choices position/alignment-choices
                          :default :middle
                          :ui {:label :string.option/alignment
                               :form-type :radio-select}}
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
              :ui {:label :string.option/origin
                   :form-type :position}}
     :direction-intermediate-name (cond-> {:point direction-intermediate-name-point-option
                                           :ui {:label :string.charge.attitude/issuant
                                                :form-type :position}}

                                    (= current-direction-intermediate-name-point
                                       :angle) (assoc :angle {:type :range
                                                              :min -180
                                                              :max 180
                                                              :default 0
                                                              :ui {:label :string.option/angle}})

                                    (not= current-direction-intermediate-name-point
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
                                              :max 80
                                              :default 45
                                              :ui {:label :string.option/angle}})

                    (not= current-orientation-point
                          :angle) (assoc :alignment {:type :choice
                                                     :choices position/alignment-choices
                                                     :default :middle
                                                     :ui {:label :string.option/alignment
                                                          :form-type :radio-select}}
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
                                                         :step 0.1}}))
     :line line-style
     :opposite-line opposite-line-style
     :extra-line extra-line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        extra-line (interface/get-sanitized-data (c/++ context :extra-line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        direction-intermediate-name (interface/get-sanitized-data (c/++ context :direction-intermediate-name))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        raw-direction-intermediate-name (interface/get-raw-data (c/++ context :direction-intermediate-name))
        direction-intermediate-name (cond-> direction-intermediate-name
                                      (-> direction-intermediate-name
                                          :point
                                          #{:left
                                            :right
                                            :top
                                            :bottom}) (->
                                                       (assoc :offset-x (or (:offset-x raw-direction-intermediate-name)
                                                                            (:offset-x origin)))
                                                       (assoc :offset-y (or (:offset-y raw-direction-intermediate-name)
                                                                            (:offset-y origin)))))
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        {direction-origin-point :real-origin
         direction-intermediate-name-point :real-orientation} (angle/calculate-origin-and-orientation
                                                               environment
                                                               origin
                                                               direction-intermediate-name
                                                               0
                                                               -90)
        pall-angle (math/normalize-angle
                    (v/angle-to-point direction-origin-point
                                      direction-intermediate-name-point))
        {origin-point :real-origin
         orientation-point :real-orientation} (angle/calculate-origin-and-orientation
                                               environment
                                               origin
                                               orientation
                                               0
                                               pall-angle)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [mirrored-origin mirrored-orientation] [(chevron/mirror-point pall-angle unadjusted-origin-point origin-point)
                                                (chevron/mirror-point pall-angle unadjusted-origin-point orientation-point)]
        origin-point (v/line-intersection origin-point orientation-point
                                          mirrored-origin mirrored-orientation)
        [relative-right relative-left] (chevron/arm-diagonals pall-angle origin-point orientation-point)
        diagonal-left (v/add origin-point relative-left)
        diagonal-right (v/add origin-point relative-right)
        direction-three (v/add origin-point (v/mul (v/add relative-left relative-right) -1))
        intersection-left (v/find-first-intersection-of-ray origin-point diagonal-left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point diagonal-right environment)
        intersection-three (v/find-first-intersection-of-ray origin-point direction-three environment)
        end-left (-> intersection-left
                     (v/sub origin-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub origin-point)
                      v/abs)
        end (max end-left end-right)
        {line-left :line
         line-left-start :line-start} (line/create line
                                                   origin-point diagonal-left
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end end
                                                   :context context
                                                   :environment environment)
        {line-right :line
         line-right-end :line-end} (line/create opposite-line
                                                origin-point diagonal-right
                                                :flipped? true
                                                :mirrored? true
                                                :real-start 0
                                                :real-end end
                                                :context context
                                                :environment environment)
        {line-three :line
         line-three-start :line-start} (line/create extra-line
                                                    origin-point intersection-three
                                                    :flipped? true
                                                    :mirrored? true
                                                    :context context
                                                    :environment environment)
        {line-three-reversed :line
         line-three-reversed-start :line-start} (line/create extra-line
                                                             origin-point intersection-three
                                                             :reversed? true
                                                             :context context
                                                             :environment environment)
        fork-infinity-points (cond
                               (<= 45 pall-angle 135) [:left :right]
                               (<= 135 pall-angle 225) [:left :left]
                               (<= 225 pall-angle 315) [:top :top]
                               :else [:right :right])
        side-infinity-points (cond
                               (<= 45 pall-angle 135) [:bottom :top]
                               (<= 135 pall-angle 225) [:left :right]
                               (<= 225 pall-angle 315) [:top :bottom]
                               :else [:right :left])
        parts [[["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 "L" origin-point
                 (path/stitch line-right)
                 (infinity/path :counter-clockwise
                                fork-infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left
                 bottom-right]]

               [["M" (v/add intersection-three
                            line-three-reversed-start)
                 (path/stitch line-three-reversed)
                 "L" origin-point
                 (path/stitch line-right)
                 (infinity/path :clockwise
                                side-infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add direction-three
                                        line-three-reversed-start)])
                 "z"]
                [top-left
                 bottom-right]]

               [["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 "L" origin-point
                 (path/stitch line-three)
                 (infinity/path :clockwise
                                (reverse side-infinity-points)
                                [(v/add direction-three
                                        line-three-start)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" (v/add direction-three
                     line-three-reversed-start)
          (path/stitch line-three-reversed)])]
       nil]
      environment]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" (v/add diagonal-left
                                line-left-start)
                     (path/stitch line-left)])}]
        [:path {:d (path/make-path
                    ["M" origin-point
                     (path/stitch line-right)])}]
        [:path {:d (path/make-path
                    ["M" origin-point
                     (path/stitch line-three)])}]])]))
