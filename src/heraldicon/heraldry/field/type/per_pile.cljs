(ns heraldicon.heraldry.field.type.per-pile
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.shared.pile :as pile]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def field-type :heraldry.field.type/per-pile)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-pile)

(defmethod field.interface/part-names field-type [_] nil)

(def ^:private size-mode-choices
  [[:string.option.size-mode-choice/thickness :thickness]
   [:string.option.size-mode-choice/angle :angle]])

(def size-mode-map
  (options/choices->map size-mode-choices))

(def ^:private orientation-type-choices
  [[:string.option.orientation-type-choice/edge :edge]
   [:string.option.orientation-type-choice/orientation-point :point]])

(def orientation-type-map
  (options/choices->map orientation-type-choices))

(defmethod field.interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
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
                                        :bottom-right])
                             :default :bottom
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (filter
                                             #(not= % current-anchor-point)
                                             [:top-left
                                              :top
                                              :top-right
                                              :left
                                              :center
                                              :right
                                              :bottom-left
                                              :bottom
                                              :bottom-right
                                              :fess
                                              :chief
                                              :base
                                              :dexter
                                              :sinister
                                              :honour
                                              :nombril
                                              :hoist
                                              :fly
                                              :angle]))
                                  :default :fess
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)
        size-mode-option {:type :option.type/choice
                          :choices size-mode-choices
                          :default :thickness
                          :ui/label :string.option/size-mode
                          :ui/element :ui.element/radio-select}
        current-size-mode (options/get-value
                           (interface/get-raw-data (c/++ context :geometry :size-mode))
                           size-mode-option)]
    {:anchor {:point anchor-point-option
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
     :orientation (cond-> {:point orientation-point-option
                           :ui/label :string.option/orientation
                           :ui/element :ui.element/position}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :option.type/range
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
     :geometry {:size-mode size-mode-option
                :size {:type :option.type/range
                       :min 5
                       :max 120
                       :default (case current-size-mode
                                  :thickness 75
                                  30)
                       :ui/label :string.option/size
                       :ui/step 0.1}
                :stretch {:type :option.type/range
                          :min 0.33
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch
                          :ui/step 0.01}
                :ui/label :string.option/geometry
                :ui/element :ui.element/geometry}}))

(defmethod interface/properties field-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-effective-parent-environment context)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        geometry (interface/get-sanitized-data (c/++ context :geometry))
        orientation (assoc orientation :type :edge)
        geometry (assoc geometry :stretch 1)
        percentage-base (if (#{:left :right} (:point anchor))
                          height
                          width)
        parent-shape (interface/get-effective-parent-shape context)
        {anchor-point :anchor
         edge-start :point
         thickness :thickness} (pile/calculate-properties
                                parent-environment
                                parent-shape
                                anchor
                                (cond-> orientation
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point anchor)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                percentage-base
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
        {left-point :left
         right-point :right} (pile/diagonals anchor-point edge-start thickness)
        edge-left-end (v/last-intersection-with-shape edge-start left-point
                                                      parent-shape :default? true)
        edge-right-end (v/last-intersection-with-shape edge-start right-point
                                                       parent-shape :default? true)
        line-length (max (v/abs (v/sub anchor-point edge-left-end))
                         (v/abs (v/sub anchor-point edge-right-end)))]
    (post-process/properties
     {:type field-type
      :edge-start edge-start
      :edge-left-end edge-left-end
      :edge-right-end edge-right-end
      :line-length line-length
      :percentage-base width
      :num-subfields 3}
     context)))

(defmethod interface/subfield-environments field-type [context {:keys []}]
  (let [{:keys [points]} (interface/get-effective-parent-environment context)
        {:keys [top-left bottom-right]} points]
    ;; TODO: replace with actual sub environments
    {:subfields [(environment/create (bb/from-points [top-left bottom-right]))
                 (environment/create (bb/from-points [top-left bottom-right]))
                 (environment/create (bb/from-points [top-left bottom-right]))]}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line opposite-line
                                                                        edge-start edge-left-end edge-right-end]}]
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
                                                    :extend-from? false
                                                    :context context)]
    {:subfields [{:shape [(shape/build-shape
                           context
                           line-edge-right
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge-left
                           line-edge-right
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge-left
                           :counter-clockwise)]}]
     :edges [{:lines [line-edge-left]}
             {:lines [line-edge-right]}]}))
