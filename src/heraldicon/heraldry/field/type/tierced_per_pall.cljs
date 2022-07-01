(ns heraldicon.heraldry.field.type.tierced-per-pall
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
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/tierced-per-pall)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-pall)

(defmethod field.interface/part-names field-type [_] ["middle" "side I" "side II"])

(defmethod field.interface/options field-type [context]
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
        origin-point-option {:type :option.type/choice
                             :choices (position/orientation-choices
                                       [:chief
                                        :base
                                        :dexter
                                        :sinister
                                        :hoist
                                        :fly
                                        :top-left
                                        :top
                                        :top-right
                                        :left
                                        :right
                                        :bottom-left
                                        :bottom
                                        :bottom-right
                                        :angle])
                             :default :top
                             :ui/label :string.option/point}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-origin-point
                                              :bottom [:bottom-left
                                                       :bottom
                                                       :bottom-right
                                                       :left
                                                       :right
                                                       :angle]
                                              :top [:top-left
                                                    :top
                                                    :top-right
                                                    :left
                                                    :right
                                                    :angle]
                                              :left [:top-left
                                                     :left
                                                     :bottom-left
                                                     :top
                                                     :bottom
                                                     :angle]
                                              :right [:top-right
                                                      :right
                                                      :bottom-right
                                                      :top
                                                      :bottom
                                                      :angle]
                                              :bottom-left [:bottom-left
                                                            :bottom
                                                            :bottom-right
                                                            :top-left
                                                            :left
                                                            :angle]
                                              :bottom-right [:bottom-left
                                                             :bottom
                                                             :bottom-right
                                                             :right
                                                             :top-right
                                                             :angle]
                                              :top-left [:top-left
                                                         :top
                                                         :top-right
                                                         :left
                                                         :bottom-left
                                                         :angle]
                                              :top-right [:top-left
                                                          :top
                                                          :top-right
                                                          :left
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
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point {:type :option.type/choice
                      :choices (position/anchor-choices
                                [:chief
                                 :base
                                 :fess
                                 :dexter
                                 :sinister
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
                      :ui/label :string.option/point}
              :alignment {:type :option.type/choice
                          :choices position/alignment-choices
                          :default :middle
                          :ui/label :string.option/alignment
                          :ui/element :ui.element/radio-select}
              :offset-x {:type :option.type/range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-x
                         :ui/step 0.1}
              :offset-y {:type :option.type/range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-y
                         :ui/step 0.1}
              :ui/label :string.option/anchor
              :ui/element :ui.element/position}
     :origin (cond-> {:point origin-point-option
                      :ui/label :string.charge.attitude/issuant
                      :ui/element :ui.element/position}

               (= current-origin-point
                  :angle) (assoc :angle {:type :option.type/range
                                         :min -180
                                         :max 180
                                         :default 0
                                         :ui/label :string.option/angle})

               (not= current-origin-point
                     :angle) (assoc :offset-x {:type :option.type/range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui/label :string.option/offset-x
                                               :ui/step 0.1}
                                    :offset-y {:type :option.type/range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui/label :string.option/offset-y
                                               :ui/step 0.1}))
     :orientation (cond-> {:point orientation-point-option
                           :ui/label :string.option/orientation
                           :ui/element :ui.element/position}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :option.type/range
                                              :min 0
                                              :max 80
                                              :default 45
                                              :ui/label :string.option/angle})

                    (not= current-orientation-point
                          :angle) (assoc :alignment {:type :option.type/choice
                                                     :choices position/alignment-choices
                                                     :default :middle
                                                     :ui/label :string.option/alignment
                                                     :ui/element :ui.element/radio-select}
                                         :offset-x {:type :option.type/range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui/label :string.option/offset-x
                                                    :ui/step 0.1}
                                         :offset-y {:type :option.type/range
                                                    :min -45
                                                    :max 45
                                                    :default 0
                                                    :ui/label :string.option/offset-y
                                                    :ui/step 0.1}))
     :line line-style
     :opposite-line opposite-line-style
     :extra-line extra-line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        extra-line (interface/get-sanitized-data (c/++ context :extra-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        origin (interface/get-sanitized-data (c/++ context :origin))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        raw-origin (interface/get-raw-data (c/++ context :origin))
        origin (cond-> origin
                 (-> origin
                     :point
                     #{:left
                       :right
                       :top
                       :bottom}) (assoc :offset-x (or (:offset-x raw-origin)
                                                      (:offset-x anchor))
                                        :offset-y (or (:offset-y raw-origin)
                                                      (:offset-y anchor))))
        points (:points environment)
        unadjusted-anchor-point (position/calculate anchor environment)
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          environment
                                          anchor
                                          origin
                                          0
                                          -90)
        pall-angle (angle/normalize
                    (v/angle-to-point direction-anchor-point
                                      origin-point))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               0
                                               pall-angle)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [mirrored-anchor mirrored-orientation] [(chevron/mirror-point pall-angle unadjusted-anchor-point anchor-point)
                                                (chevron/mirror-point pall-angle unadjusted-anchor-point orientation-point)]
        anchor-point (v/line-intersection anchor-point orientation-point
                                          mirrored-anchor mirrored-orientation)
        [relative-right relative-left] (chevron/arm-diagonals pall-angle anchor-point orientation-point)
        diagonal-left (v/add anchor-point relative-left)
        diagonal-right (v/add anchor-point relative-right)
        direction-three (v/add anchor-point (v/mul (v/add relative-left relative-right) -1))
        intersection-left (v/find-first-intersection-of-ray anchor-point diagonal-left environment)
        intersection-right (v/find-first-intersection-of-ray anchor-point diagonal-right environment)
        intersection-three (v/find-first-intersection-of-ray anchor-point direction-three environment)
        end-left (-> intersection-left
                     (v/sub anchor-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub anchor-point)
                      v/abs)
        end (max end-left end-right)
        {line-left :line
         line-left-start :line-start} (line/create line
                                                   anchor-point diagonal-left
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end end
                                                   :context context
                                                   :environment environment)
        {line-right :line
         line-right-end :line-end} (line/create opposite-line
                                                anchor-point diagonal-right
                                                :flipped? true
                                                :mirrored? true
                                                :real-start 0
                                                :real-end end
                                                :context context
                                                :environment environment)
        {line-three :line
         line-three-start :line-start} (line/create extra-line
                                                    anchor-point intersection-three
                                                    :flipped? true
                                                    :mirrored? true
                                                    :context context
                                                    :environment environment)
        {line-three-reversed :line
         line-three-reversed-start :line-start} (line/create extra-line
                                                             anchor-point intersection-three
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
                 "L" anchor-point
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
                 "L" anchor-point
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
                 "L" anchor-point
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
                    ["M" anchor-point
                     (path/stitch line-right)])}]
        [:path {:d (path/make-path
                    ["M" anchor-point
                     (path/stitch line-three)])}]])]))
