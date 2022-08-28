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
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

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
  (let [parent-environment (interface/get-parent-environment context)
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
        parent-shape (interface/get-exact-parent-shape context)
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
      :num-subfields 8}
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
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        {line-edge-top-left :line
         line-edge-top-left-start :line-start
         line-edge-top-left-to :adjusted-to
         :as line-edge-top-left-data} (line/create-with-extension line
                                                                  edge-top-left-1 edge-top-left-2
                                                                  bounding-box
                                                                  :reversed? true
                                                                  :extend-from? false
                                                                  :context context)
        {line-edge-bottom-right :line
         line-edge-bottom-right-start :line-start
         line-edge-bottom-right-to :adjusted-to
         :as line-edge-bottom-right-data} (line/create-with-extension line
                                                                      edge-bottom-right-1 edge-bottom-right-2
                                                                      bounding-box
                                                                      :reversed? true
                                                                      :extend-from? false
                                                                      :context context)
        {line-edge-bottom-left :line
         line-edge-bottom-left-start :line-start
         line-edge-bottom-left-to :adjusted-to
         :as line-edge-bottom-left-data} (line/create-with-extension line
                                                                     edge-bottom-left-1 edge-bottom-left-2
                                                                     bounding-box
                                                                     :reversed? true
                                                                     :extend-from? false
                                                                     :context context)
        {line-edge-top-right :line
         line-edge-top-right-start :line-start
         line-edge-top-right-to :adjusted-to
         :as line-edge-top-right-data} (line/create-with-extension line
                                                                   edge-top-right-1 edge-top-right-2
                                                                   bounding-box
                                                                   :reversed? true
                                                                   :extend-from? false
                                                                   :context context)
        {line-edge-top :line
         line-edge-top-from :adjusted-from
         line-edge-top-to :adjusted-to
         :as line-edge-top-data} (line/create-with-extension opposite-line
                                                             edge-top-1 edge-top-2
                                                             bounding-box
                                                             :mirrored? true
                                                             :flipped? true
                                                             :extend-from? false
                                                             :context context)
        {line-edge-bottom :line
         line-edge-bottom-from :adjusted-from
         line-edge-bottom-to :adjusted-to
         :as line-edge-bottom-data} (line/create-with-extension opposite-line
                                                                edge-bottom-1 edge-bottom-2
                                                                bounding-box
                                                                :mirrored? true
                                                                :flipped? true
                                                                :extend-from? false
                                                                :context context)
        {line-edge-left :line
         line-edge-left-from :adjusted-from
         line-edge-left-to :adjusted-to
         :as line-edge-left-data} (line/create-with-extension opposite-line
                                                              edge-left-1 edge-left-2
                                                              bounding-box
                                                              :mirrored? true
                                                              :flipped? true
                                                              :extend-from? false
                                                              :context context)
        {line-edge-right :line
         line-edge-right-from :adjusted-from
         line-edge-right-to :adjusted-to
         :as line-edge-right-data} (line/create-with-extension opposite-line
                                                               edge-right-1 edge-right-2
                                                               bounding-box
                                                               :mirrored? true
                                                               :flipped? true
                                                               :extend-from? false
                                                               :context context)]
    {:subfields [{:shape [(path/make-path
                           ["M" (v/add line-edge-top-left-to line-edge-top-left-start)
                            (path/stitch line-edge-top-left)
                            (path/stitch line-edge-top)
                            (infinity/counter-clockwise
                             line-edge-top-to
                             (v/add line-edge-top-left-to line-edge-top-left-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-top-right-to line-edge-top-right-start)
                            (path/stitch line-edge-top-right)
                            (path/stitch line-edge-top)
                            (infinity/clockwise
                             line-edge-top-to
                             (v/add line-edge-top-right-to line-edge-top-right-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-top-left-to line-edge-top-left-start)
                            (path/stitch line-edge-top-left)
                            (path/stitch line-edge-left)
                            (infinity/clockwise
                             line-edge-left-to
                             (v/add line-edge-top-left-to line-edge-top-left-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-top-right-to line-edge-top-right-start)
                            (path/stitch line-edge-top-right)
                            (path/stitch line-edge-right)
                            (infinity/counter-clockwise
                             line-edge-right-to
                             (v/add line-edge-top-right-to line-edge-top-right-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-bottom-left-to line-edge-bottom-left-start)
                            (path/stitch line-edge-bottom-left)
                            (path/stitch line-edge-left)
                            (infinity/counter-clockwise
                             line-edge-left-to
                             (v/add line-edge-bottom-left-to line-edge-bottom-left-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-bottom-right-to line-edge-bottom-right-start)
                            (path/stitch line-edge-bottom-right)
                            (path/stitch line-edge-right)
                            (infinity/clockwise
                             line-edge-right-to
                             (v/add line-edge-bottom-right-to line-edge-bottom-right-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-bottom-left-to line-edge-bottom-left-start)
                            (path/stitch line-edge-bottom-left)
                            (path/stitch line-edge-bottom)
                            (infinity/clockwise
                             line-edge-bottom-to
                             (v/add line-edge-bottom-left-to line-edge-bottom-left-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-bottom-right-to line-edge-bottom-right-start)
                            (path/stitch line-edge-bottom-right)
                            (path/stitch line-edge-bottom)
                            (infinity/counter-clockwise
                             line-edge-bottom-to
                             (v/add line-edge-bottom-right-to line-edge-bottom-right-start))
                            "z"])]}]
     :lines [{:line line
              :line-from line-edge-top-left-to
              :line-data [line-edge-top-left-data]}
             {:line line
              :line-from line-edge-bottom-right-to
              :line-data [line-edge-bottom-right-data]}
             {:line line
              :line-from line-edge-bottom-left-to
              :line-data [line-edge-bottom-left-data]}
             {:line line
              :line-from line-edge-top-right-to
              :line-data [line-edge-top-right-data]}
             {:line opposite-line
              :line-from line-edge-top-from
              :line-data [line-edge-top-data]}
             {:line opposite-line
              :line-from line-edge-bottom-from
              :line-data [line-edge-bottom-data]}
             {:line opposite-line
              :line-from line-edge-left-from
              :line-data [line-edge-left-data]}
             {:line opposite-line
              :line-from line-edge-right-from
              :line-data [line-edge-right-data]}]}))
