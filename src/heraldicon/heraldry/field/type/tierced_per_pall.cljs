(ns heraldicon.heraldry.field.type.tierced-per-pall
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
     :opposite-line opposite-line-style
     :extra-line extra-line-style}))

(defmethod interface/properties field-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-effective-parent-environment context)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        origin (interface/get-sanitized-data (c/++ context :origin))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
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
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          parent-environment
                                          anchor
                                          origin
                                          0
                                          -90)
        pall-angle (angle/normalize
                    (v/angle-to-point direction-anchor-point
                                      origin-point))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               0
                                               pall-angle)
        ;; left/right are based on the chevron view
        [relative-left relative-right] (chevron/arm-diagonals pall-angle anchor-point orientation-point)
        relative-bottom (v/mul (v/add relative-left relative-right) -1)
        parent-shape (interface/get-exact-parent-shape context)
        edge-bottom-end (v/last-intersection-with-shape anchor-point relative-bottom
                                                        parent-shape :default? true :relative? true)
        edge-left-end (v/last-intersection-with-shape anchor-point relative-left
                                                      parent-shape :default? true :relative? true)
        edge-right-end (v/last-intersection-with-shape anchor-point relative-right
                                                       parent-shape :default? true :relative? true)
        line-length (->> (map (fn [v]
                                (v/abs (v/sub v anchor-point)))
                              [edge-bottom-end edge-left-end edge-right-end])
                         (apply max))]
    (post-process/properties
     {:type field-type
      :edge-start anchor-point
      :edge-bottom-end edge-bottom-end
      :edge-left-end edge-left-end
      :edge-right-end edge-right-end
      :line-length line-length
      :percentage-base (min width height)
      :num-subfields 3}
     context)))

(defmethod interface/subfield-environments field-type [_context {:keys [edge-start edge-bottom-end
                                                                        edge-left-end edge-right-end]}]
  ;; TODO: include outer environment points, based on rotation
  {:subfields [(environment/create (bb/from-points [edge-start edge-left-end edge-right-end]))
               (environment/create (bb/from-points [edge-start edge-left-end edge-bottom-end]))
               (environment/create (bb/from-points [edge-start edge-right-end edge-bottom-end]))]})

(defmethod interface/subfield-render-shapes field-type [context {:keys [line opposite-line extra-line
                                                                        edge-start edge-bottom-end
                                                                        edge-left-end edge-right-end]}]
  (let [{:keys [bounding-box]} (interface/get-effective-parent-environment context)
        line-edge-left (line/create-with-extension line
                                                   edge-start edge-left-end
                                                   bounding-box
                                                   :reversed? true
                                                   :extend-from? false
                                                   :context context)
        line-edge-right (line/create-with-extension opposite-line
                                                    edge-start edge-right-end
                                                    bounding-box
                                                    :flipped? true
                                                    :mirrored? true
                                                    :extend-from? false
                                                    :context context)
        line-edge-bottom (line/create-with-extension extra-line
                                                     edge-start edge-bottom-end
                                                     bounding-box
                                                     :reversed? true
                                                     :extend-from? false
                                                     :context context)]
    {:subfields [{:shape [(shape/build-shape
                           context
                           line-edge-left
                           line-edge-right
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge-left
                           [:reverse line-edge-bottom]
                           :counter-clockwise)]}

                 {:shape [(shape/build-shape
                           context
                           line-edge-bottom
                           line-edge-right
                           :counter-clockwise)]}]
     :edges [{:lines [line-edge-left]}
             {:lines [line-edge-right]}
             {:lines [line-edge-bottom]}]}))
