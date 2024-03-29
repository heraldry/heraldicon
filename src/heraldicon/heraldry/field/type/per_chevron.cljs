(ns heraldicon.heraldry.field.type.per-chevron
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.shared.chevron :as chevron]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

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
        origin-point-option {:type :option.type/choice
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
                             :ui/label :string.option/point}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        orientation-point-option {:type :option.type/choice
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
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point {:type :option.type/choice
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
                      :ui/label :string.option/point}
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
                                              :max 360
                                              :default 45
                                              :ui/label :string.option/angle})

                    (not= current-orientation-point
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
     :line line-style
     :opposite-line opposite-line-style}))

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-subfields-environment context)
        sinister? (= (interface/get-raw-data (c/++ context :type))
                     :heraldry.field.type/per-bend-sinister)
        percentage-base (:height parent-environment)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        origin (interface/get-sanitized-data (c/++ context :origin))
        origin (update origin :point (fn [origin-point]
                                       (get {:chief :top
                                             :base :bottom
                                             :dexter :left
                                             :sinister :right} origin-point origin-point)))
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
        unadjusted-anchor-point (position/calculate anchor parent-environment)
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          parent-environment
                                          anchor
                                          origin
                                          0
                                          90)
        chevron-angle (angle/normalize
                       (v/angle-to-point direction-anchor-point
                                         origin-point))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               0
                                               chevron-angle)
        [mirrored-anchor mirrored-orientation] [(chevron/mirror-point chevron-angle unadjusted-anchor-point anchor-point)
                                                (chevron/mirror-point chevron-angle unadjusted-anchor-point orientation-point)]
        anchor-point (v/line-intersection anchor-point orientation-point
                                          mirrored-anchor mirrored-orientation)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle anchor-point orientation-point)
        angle-left (angle/normalize (v/angle-to-point v/zero relative-left))
        angle-right (angle/normalize (v/angle-to-point v/zero relative-right))
        joint-angle (angle/normalize (- angle-left angle-right))
        parent-shape (interface/get-subfields-shape context)
        edge-left (v/last-intersection-with-shape anchor-point relative-left parent-shape
                                                  :default? true :relative? true)
        edge-right (v/last-intersection-with-shape anchor-point relative-right parent-shape
                                                   :default? true :relative? true)
        line-length (max (v/abs (v/sub anchor-point edge-left))
                         (v/abs (v/sub anchor-point edge-right)))]
    (post-process/properties
     {:type field-type
      :sinister? sinister?
      :edge [edge-left anchor-point edge-right]
      :chevron-angle chevron-angle
      :joint-angle joint-angle
      :line-length line-length
      :percentage-base percentage-base
      :num-subfields 2}
     context)))

(defmethod interface/subfield-environments field-type [context]
  (let [{[edge-left edge-corner edge-right] :edge} (interface/get-properties context)
        {:keys [points]} (interface/get-subfields-environment context)
        {:keys [top bottom]} points]
    ;; TODO: needs to be smarter with chevron-angle
    {:subfields [(environment/create (bb/from-points [edge-corner top
                                                      edge-left edge-right]))
                 (environment/create (bb/from-points [edge-corner bottom
                                                      edge-left edge-right]))]}))

(defmethod interface/subfield-render-shapes field-type [context]
  (let [{:keys [line opposite-line]
         [edge-left edge-corner edge-right] :edge} (interface/get-properties context)
        {:keys [bounding-box]} (interface/get-subfields-environment context)
        line-edge-left (line/create-with-extension context
                                                   line
                                                   edge-corner edge-left
                                                   bounding-box
                                                   :reversed? true
                                                   :extend-from? false)
        line-edge-right (line/create-with-extension context
                                                    opposite-line
                                                    edge-corner edge-right
                                                    bounding-box
                                                    :extend-from? false)]
    {:subfields [{:shape [(shape/build-shape
                           (c/++ context :fields 0)
                           line-edge-left
                           line-edge-right
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 1)
                           line-edge-left
                           line-edge-right
                           :clockwise)]}]

     :edges [{:lines [line-edge-left]}
             {:lines [line-edge-right]}]}))
