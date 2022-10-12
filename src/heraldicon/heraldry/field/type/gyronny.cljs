(ns heraldicon.heraldry.field.type.gyronny
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.shared.saltire :as saltire]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def field-type :heraldry.field.type/gyronny)

(defmethod field.interface/display-name field-type [_] :string.field.type/gyronny)

(defmethod field.interface/part-names field-type [_] ["I" "II" "III" "IV" "V" "VI" "VII" "VIII"])

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
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            [:top-left
                                             :top-right
                                             :bottom-left
                                             :bottom-right
                                             :angle])
                                  :default :top-left
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
                                 :center])
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
     :orientation (cond-> {:point orientation-point-option
                           :ui/label :string.option/orientation
                           :ui/element :ui.element/position}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :option.type/range
                                              :min 10
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
     :opposite-line opposite-line-style}))

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-subfields-environment context)
        percentage-base (:height parent-environment)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               0
                                               nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals anchor-point orientation-point)
        parent-shape (interface/get-subfields-shape context)
        top-left-end (v/last-intersection-with-shape anchor-point relative-top-left
                                                     parent-shape :default? true :relative? true)
        top-right-end (v/last-intersection-with-shape anchor-point relative-top-right
                                                      parent-shape :default? true :relative? true)
        bottom-left-end (v/last-intersection-with-shape anchor-point relative-bottom-left
                                                        parent-shape :default? true :relative? true)
        bottom-right-end (v/last-intersection-with-shape anchor-point relative-bottom-right
                                                         parent-shape :default? true :relative? true)
        left-end (v/last-intersection-with-shape anchor-point (v/Vector. -50 0)
                                                 parent-shape :default? true :relative? true)
        right-end (v/last-intersection-with-shape anchor-point (v/Vector. 50 0)
                                                  parent-shape :default? true :relative? true)
        top-end (v/last-intersection-with-shape anchor-point (v/Vector. 0 -50)
                                                parent-shape :default? true :relative? true)
        bottom-end (v/last-intersection-with-shape anchor-point (v/Vector. 0 50)
                                                   parent-shape :default? true :relative? true)
        line-length (->> [top-end bottom-end
                          left-end right-end
                          top-left-end top-right-end
                          bottom-left-end bottom-right-end]
                         (map (fn [v]
                                (v/sub v anchor-point)))
                         (map v/abs)
                         (apply max))]
    (post-process/properties
     {:type field-type
      :edge-left [anchor-point left-end]
      :edge-right [anchor-point right-end]
      :edge-top [anchor-point top-end]
      :edge-bottom [anchor-point bottom-end]
      :edge-top-left [anchor-point top-left-end]
      :edge-top-right [anchor-point top-right-end]
      :edge-bottom-left [anchor-point bottom-left-end]
      :edge-bottom-right [anchor-point bottom-right-end]
      :line-length line-length
      :percentage-base percentage-base
      :num-subfields 8
      :overlap?-fn #{0 3 4 7}}
     context)))

(defmethod interface/subfield-environments field-type [_context {[middle edge-top-2] :edge-top
                                                                 [_middle edge-bottom-2] :edge-bottom
                                                                 [_middle edge-left-2] :edge-left
                                                                 [_middle edge-right-2] :edge-right
                                                                 [_middle edge-top-left-2] :edge-top-left
                                                                 [_middle edge-top-right-2] :edge-top-right
                                                                 [_middle edge-bottom-left-2] :edge-bottom-left
                                                                 [_middle edge-bottom-right-2] :edge-bottom-right}]
  {:subfields [(let [points [middle edge-top-left-2 edge-top-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
               (let [points [middle edge-top-right-2 edge-top-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
               (let [points [middle edge-top-left-2 edge-left-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
               (let [points [middle edge-top-right-2 edge-right-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
               (let [points [middle edge-bottom-left-2 edge-left-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
               (let [points [middle edge-bottom-right-2 edge-right-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
               (let [points [middle edge-bottom-left-2 edge-bottom-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
               (let [points [middle edge-bottom-right-2 edge-bottom-2]]
                 (environment/create (bb/from-points points) {:fess (apply v/avg points)}))]})

(defmethod interface/subfield-render-shapes field-type [context {:keys [line opposite-line]
                                                                 [edge-top-1 edge-top-2] :edge-top
                                                                 [edge-bottom-1 edge-bottom-2] :edge-bottom
                                                                 [edge-left-1 edge-left-2] :edge-left
                                                                 [edge-right-1 edge-right-2] :edge-right
                                                                 [edge-top-left-1 edge-top-left-2] :edge-top-left
                                                                 [edge-top-right-1 edge-top-right-2] :edge-top-right
                                                                 [edge-bottom-left-1 edge-bottom-left-2] :edge-bottom-left
                                                                 [edge-bottom-right-1 edge-bottom-right-2] :edge-bottom-right}]
  (let [{:keys [bounding-box]} (interface/get-subfields-environment context)
        line-edge-top-left (line/create-with-extension context
                                                       line
                                                       edge-top-left-1 edge-top-left-2
                                                       bounding-box
                                                       :reversed? true
                                                       :extend-from? false)
        line-edge-bottom-right (line/create-with-extension context
                                                           line
                                                           edge-bottom-right-1 edge-bottom-right-2
                                                           bounding-box
                                                           :reversed? true
                                                           :extend-from? false)
        line-edge-bottom-left (line/create-with-extension context
                                                          line
                                                          edge-bottom-left-1 edge-bottom-left-2
                                                          bounding-box
                                                          :reversed? true
                                                          :extend-from? false)
        line-edge-top-right (line/create-with-extension context
                                                        line
                                                        edge-top-right-1 edge-top-right-2
                                                        bounding-box
                                                        :reversed? true
                                                        :extend-from? false)
        line-edge-top (line/create-with-extension context
                                                  opposite-line
                                                  edge-top-1 edge-top-2
                                                  bounding-box
                                                  :mirrored? true
                                                  :flipped? true
                                                  :extend-from? false)
        line-edge-bottom (line/create-with-extension context
                                                     opposite-line
                                                     edge-bottom-1 edge-bottom-2
                                                     bounding-box
                                                     :mirrored? true
                                                     :flipped? true
                                                     :extend-from? false)
        line-edge-left (line/create-with-extension context
                                                   opposite-line
                                                   edge-left-1 edge-left-2
                                                   bounding-box
                                                   :mirrored? true
                                                   :flipped? true
                                                   :extend-from? false)
        line-edge-right (line/create-with-extension context
                                                    opposite-line
                                                    edge-right-1 edge-right-2
                                                    bounding-box
                                                    :mirrored? true
                                                    :flipped? true
                                                    :extend-from? false)]
    {:subfields [{:shape [(shape/build-shape
                           (c/++ context :fields 0)
                           line-edge-top-left
                           line-edge-top
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 1)
                           line-edge-top-right
                           line-edge-top
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 2)
                           line-edge-top-left
                           line-edge-left
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 3)
                           line-edge-top-right
                           line-edge-right
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 4)
                           line-edge-bottom-left
                           line-edge-left
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 5)
                           line-edge-bottom-right
                           line-edge-right
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 6)
                           line-edge-bottom-left
                           line-edge-bottom
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 7)
                           line-edge-bottom-right
                           line-edge-bottom
                           :counter-clockwise)]}]
     :edges [{:lines [line-edge-top-left]}
             {:lines [line-edge-bottom-right]}
             {:lines [line-edge-bottom-left]}
             {:lines [line-edge-top-right]}
             {:lines [line-edge-top]}
             {:lines [line-edge-bottom]}
             {:lines [line-edge-left]}
             {:lines [line-edge-right]}]}))
