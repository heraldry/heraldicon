(ns heraldicon.heraldry.field.type.per-chevron
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.shared.chevron :as chevron]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/per-chevron)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-chevron)

(defmethod field.interface/part-names field-type [_] ["chief" "base"])

(defmethod field.interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        origin-point-option {:type :choice
                             :choices (position/orientation-choices
                                       [:chief
                                        :base
                                        :dexter
                                        :sinister
                                        :hoist
                                        :fly
                                        :top-left
                                        :top-right
                                        :bottom-left
                                        :bottom-right
                                        :angle])
                             :default :base
                             :ui {:label :string.option/point}}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        orientation-point-option {:type :choice
                                  :choices (position/orientation-choices
                                            (case current-origin-point
                                              :base [:bottom-left
                                                     :bottom-right
                                                     :left
                                                     :right
                                                     :angle]
                                              :chief [:top-left
                                                      :top-right
                                                      :left
                                                      :right
                                                      :angle]
                                              :dexter [:top-left
                                                       :bottom-left
                                                       :top
                                                       :bottom
                                                       :angle]
                                              :sinister [:top-right
                                                         :bottom-right
                                                         :top
                                                         :bottom
                                                         :angle]
                                              :bottom-left [:bottom
                                                            :bottom-right
                                                            :top-left
                                                            :left
                                                            :angle]
                                              :bottom-right [:bottom-left
                                                             :bottom
                                                             :right
                                                             :top-right
                                                             :angle]
                                              :top-left [:top
                                                         :top-right
                                                         :left
                                                         :bottom-left
                                                         :angle]
                                              :top-right [:top-left
                                                          :top
                                                          :right
                                                          :bottom-right
                                                          :angle]
                                              [:top-left
                                               :top
                                               :top-right
                                               :left
                                               :right
                                               :bottom-left
                                               :bottom
                                               :bottom-right
                                               :angle]))
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
                      :choices (position/anchor-choices
                                [:fess
                                 :chief
                                 :base
                                 :honour
                                 :nombril
                                 :hoist
                                 :fly
                                 :top-left
                                 :top
                                 :top-right
                                 :left
                                 :center
                                 :right
                                 :bottom-left
                                 :bottom
                                 :bottom-right])
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
     :opposite-line opposite-line-style}))

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
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          environment
                                          anchor
                                          origin
                                          0
                                          90)
        chevron-angle (angle/normalize
                       (v/angle-to-point direction-anchor-point
                                         origin-point))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
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
