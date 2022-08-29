(ns heraldicon.heraldry.field.type.chevronny
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.shared.chevron :as chevron]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/chevronny)

(defmethod field.interface/display-name field-type [_] :string.field.type/chevronny)

(defmethod field.interface/part-names field-type [_] nil)

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
                                             :angle])
                                  :default :angle
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:orientation (cond-> {:point orientation-point-option
                           :ui/label :string.option/orientation
                           :ui/element :ui.element/position}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :option.type/range
                                              :min 10
                                              :max 170
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
     :layout {:num-fields-y {:type :option.type/range
                             :min 1
                             :max 20
                             :default 6
                             :integer? true
                             :ui/label :string.option/subfields-y
                             :ui/element :ui.element/field-layout-num-fields-y}
              :num-base-fields {:type :option.type/range
                                :min 2
                                :max 8
                                :default 2
                                :integer? true
                                :ui/label :string.option/base-fields
                                :ui/element :ui.element/field-layout-num-base-fields}
              :offset-y {:type :option.type/range
                         :min -3
                         :max 3
                         :default 0
                         :ui/label :string.option/offset-y
                         :ui/step 0.01}
              :stretch-y {:type :option.type/range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch-y
                          :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style
     :opposite-line opposite-line-style}))

(defmethod interface/properties field-type [context]
  (let [{:keys [height points]
         :as parent-environment} (interface/get-parent-environment context)
        {:keys [center]} points
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        chevron-angle 90
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               {:point :fess
                                                :offset-x 0
                                                :offset-y 0}
                                               orientation
                                               0 ;; ignored, since there's no alignment
                                               chevron-angle)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle anchor-point orientation-point)
        part-height (-> height
                        (/ (dec num-fields-y))
                        (* stretch-y))
        required-height (* part-height
                           (dec num-fields-y))
        y0 (-> (:y center)
               (- (/ required-height 2))
               (+ (* offset-y
                     part-height)))
        corner-x (:x anchor-point)
        parent-shape (interface/get-exact-parent-shape context)
        edges (mapv (fn [i]
                      (let [corner-y (+ y0 (* (dec i) part-height))
                            corner-point (v/Vector. corner-x corner-y)
                            edge-start (v/last-intersection-with-shape corner-point relative-left parent-shape
                                                                       :default? true :relative? true)
                            edge-end (v/last-intersection-with-shape corner-point relative-right parent-shape
                                                                     :default? true :relative? true)]
                        [edge-start corner-point edge-end]))
                    (range num-fields-y))
        line-length (apply max (mapcat (fn [[edge-start corner-point edge-end]]
                                         [(v/abs (v/sub edge-start corner-point))
                                          (v/abs (v/sub edge-end corner-point))])
                                       edges))]
    (post-process/properties
     {:type field-type
      :edges edges
      :part-height part-height
      :line-length line-length
      :num-subfields num-fields-y}
     context)))

(defmethod interface/subfield-environments field-type [context {:keys [edges part-height]}]
  (let [{:keys [points]} (interface/get-parent-environment context)
        {:keys [left right]} points
        offset (v/Vector. 0 part-height)]
    ;; TODO: needs to be improved
    {:subfields (mapv (fn [[edge-start corner-point edge-end]]
                        (environment/create (bb/from-points [corner-point (v/add corner-point offset)
                                                             (assoc edge-start :x (:x left))
                                                             (assoc (v/add edge-start offset) :x (:x right))
                                                             (assoc edge-end :x (:x left))
                                                             (assoc (v/add edge-end offset) :x (:x right))])))
                      edges)}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line opposite-line edges num-subfields]}]
  (let [{:keys [bounding-box points]} (interface/get-parent-environment context)
        {:keys [top-left bottom-right]} points
        outside-1 (v/sub top-left (v/Vector. 50 50))
        outside-2 (v/add bottom-right (v/Vector. 50 50))
        lines (mapv (fn [[edge-start corner-point edge-end]]
                      [(line/create-with-extension line
                                                   corner-point edge-start
                                                   bounding-box
                                                   :reversed? true
                                                   :extend-from? false
                                                   :context context)
                       (line/create-with-extension opposite-line
                                                   corner-point edge-end
                                                   bounding-box
                                                   :reversed? true
                                                   :flipped? true
                                                   :mirrored? true
                                                   :extend-from? false
                                                   :context context)])
                    (drop 1 edges))]
    {:subfields (into []
                      (comp
                       (map (fn [i]
                              (let [first? (zero? i)
                                    last? (= i (dec num-subfields))]
                                (cond
                                  (and first?
                                       last?) ["M" outside-1
                                               ;; do this in two steps, because using the same point
                                               ;; wouldn't use the large arc
                                               (infinity/clockwise bounding-box outside-1 outside-2)
                                               (infinity/clockwise bounding-box outside-2 outside-1)
                                               "z"]
                                  first? (let [[line-1-left line-1-right] (get lines i)
                                               line-1-start (v/add (:adjusted-to line-1-left) (:line-start line-1-left))
                                               line-1-end (:adjusted-to line-1-right)]
                                           ["M" line-1-start
                                            (path/stitch (:line line-1-left))
                                            (path/stitch (line/reversed-path (:line line-1-right)))
                                            (infinity/counter-clockwise bounding-box line-1-end line-1-start :shortest? true)
                                            "z"])
                                  last? (let [[line-1-left line-1-right] (get lines (dec i))
                                              [line-1-start line-1-end] [(v/add (:adjusted-to line-1-left) (:line-start line-1-left))
                                                                         (:adjusted-to line-1-right)]]
                                          ["M" line-1-start
                                           (path/stitch (:line line-1-left))
                                           (path/stitch (line/reversed-path (:line line-1-right)))
                                           (infinity/clockwise bounding-box line-1-end line-1-start :shortest? true)
                                           "z"])
                                  :else (let [[line-1-left line-1-right] (get lines (dec i))
                                              ;; swap arms for the lower line
                                              [line-2-right line-2-left] (get lines i)
                                              line-1-start (v/add (:adjusted-to line-1-left) (:line-start line-1-left))
                                              line-1-end (:adjusted-to line-1-right)
                                              line-2-start (v/add (:adjusted-to line-2-left) (:line-start line-2-left))
                                              line-2-end (:adjusted-to line-2-right)]
                                          ["M" line-1-start
                                           (path/stitch (:line line-1-left))
                                           (path/stitch (line/reversed-path (:line line-1-right)))
                                           (infinity/clockwise bounding-box line-1-end line-2-start :shortest? true)
                                           (path/stitch (:line line-2-left))
                                           (path/stitch (line/reversed-path (:line line-2-right)))
                                           (infinity/clockwise bounding-box line-2-end line-1-start :shortest? true)
                                           "z"])))))
                       (map (fn [path]
                              {:shape [(path/make-path path)]})))
                      (range num-subfields))
     :lines (vec (mapcat (fn [[line-left line-right]]
                           [{:line (:line line-left)
                             :line-from (:adjusted-to line-left)
                             :line-data [line-left]}
                            {:line (:line line-right)
                             :line-from (:adjusted-to line-right)
                             :line-data [line-right]}])
                         lines))}))
