(ns heraldicon.heraldry.field.type.paly
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/paly)

(defmethod field.interface/display-name field-type [_] :string.field.type/paly)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)]
    {:layout {:num-fields-x {:type :option.type/range
                             :min 1
                             :max 20
                             :default 6
                             :integer? true
                             :ui/label :string.option/subfields-x
                             :ui/element :ui.element/field-layout-num-fields-x}
              :num-base-fields {:type :option.type/range
                                :min 2
                                :max 8
                                :default 2
                                :integer? true
                                :ui/label :string.option/base-fields
                                :ui/element :ui.element/field-layout-num-base-fields}
              :offset-x {:type :option.type/range
                         :min -1
                         :max 1
                         :default 0
                         :ui/label :string.option/offset-x
                         :ui/step 0.01}
              :stretch-x {:type :option.type/range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch-x
                          :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style}))

(defmethod interface/properties field-type [context]
  (let [{:keys [width points]} (interface/get-parent-environment context)
        {:keys [center top bottom]} points
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        required-width (* part-width
                          num-fields-x)
        x0 (-> (:x center)
               (- (/ required-width 2))
               (+ (* offset-x
                     part-width)))
        parent-shape (interface/get-exact-parent-shape context)
        edges (mapv (fn [i]
                      (let [edge-x (+ x0 (* i part-width))]
                        (v/intersections-with-shape
                         (v/Vector. edge-x (:y top)) (v/Vector. edge-x (:y bottom))
                         parent-shape :default? true)))
                    (range num-fields-x))
        ;; the second edge is the first one visible, so drop the first
        start-y (->> (drop 1 edges)
                     (map (comp :y first))
                     (apply min))
        end-y (apply max (map (comp :y second) edges))
        edges (mapv (fn [[edge-start edge-end]]
                      [(assoc edge-start :y start-y)
                       (assoc edge-end :y end-y)])
                    edges)
        line-length (apply max (map (fn [[edge-start edge-end]]
                                      (v/abs (v/sub edge-end edge-start)))
                                    edges))]
    (post-process/properties
     {:type field-type
      :edges edges
      :part-width part-width
      :start-y start-y
      :end-y end-y
      :line-length line-length
      :num-subfields num-fields-x}
     context)))

(defmethod interface/subfield-environments field-type [_context {:keys [edges start-y end-y part-width]}]
  {:subfields (mapv (fn [[edge-start _edge-end]]
                      (environment/create (bb/from-points [(assoc edge-start :y start-y)
                                                           (assoc edge-start
                                                                  :x (+ (:x edge-start) part-width)
                                                                  :y end-y)])))
                    edges)})

(defmethod interface/subfield-render-shapes field-type [context {:keys [line edges num-subfields]}]
  (let [{:keys [bounding-box points]} (interface/get-parent-environment context)
        {:keys [top-left bottom-right]} points
        outside-1 (v/sub top-left (v/Vector. 50 50))
        outside-2 (v/add bottom-right (v/Vector. 50 50))
        lines (vec (map-indexed
                    (fn [index [edge-start edge-end]]
                      (if (even? index)
                        (line/create-with-extension line
                                                    edge-start edge-end
                                                    bounding-box
                                                    :context context)
                        (line/create-with-extension line
                                                    edge-start edge-end
                                                    bounding-box
                                                    :reversed? true
                                                    :flipped? true
                                                    :mirrored? true
                                                    :context context)))
                    (drop 1 edges)))]
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
                                  first? (let [line-1 (get lines i)
                                               line-start (v/add (:adjusted-from line-1) (:line-start line-1))
                                               line-end (:adjusted-to line-1)]
                                           ["M" line-start
                                            (path/stitch (:line line-1))
                                            (infinity/clockwise bounding-box line-end line-start :shortest? true)
                                            "z"])
                                  last? (let [line-1 (get lines (dec i))
                                              even-line? (even? (dec i))
                                              [line-start line-end] (if even-line?
                                                                      [(v/add (:adjusted-from line-1) (:line-start line-1))
                                                                       (:adjusted-to line-1)]
                                                                      [(v/add (:adjusted-to line-1) (:line-start line-1))
                                                                       (:adjusted-from line-1)])]
                                          ["M" line-start
                                           (path/stitch (:line line-1))
                                           ((if even-line?
                                              infinity/counter-clockwise
                                              infinity/clockwise) bounding-box line-end line-start :shortest? true)
                                           "z"])
                                  :else (let [even-part? (even? i)
                                              line-1 (get lines (dec i))
                                              line-2 (get lines i)]
                                          (if even-part?
                                            (let [line-1-start (v/add (:adjusted-to line-1) (:line-start line-1))
                                                  line-1-end (:adjusted-from line-1)
                                                  line-2-start (v/add (:adjusted-from line-2) (:line-start line-2))
                                                  line-2-end (:adjusted-to line-2)]
                                              ["M" line-1-start
                                               (path/stitch (:line line-1))
                                               (infinity/clockwise bounding-box line-1-end line-2-start :shortest? true)
                                               (path/stitch (:line line-2))
                                               (infinity/clockwise bounding-box line-2-end line-1-start :shortest? true)
                                               "z"])
                                            (let [line-1-start (v/add (:adjusted-from line-1) (:line-start line-1))
                                                  line-1-end (:adjusted-to line-1)
                                                  line-2-start (v/add (:adjusted-to line-2) (:line-start line-2))
                                                  line-2-end (:adjusted-from line-2)]
                                              ["M" line-1-start
                                               (path/stitch (:line line-1))
                                               (infinity/counter-clockwise bounding-box line-1-end line-2-start :shortest? true)
                                               (path/stitch (:line line-2))
                                               (infinity/counter-clockwise bounding-box line-2-end line-1-start :shortest? true)
                                               "z"])))))))
                       (map (fn [path]
                              {:shape [(path/make-path path)]})))
                      (range num-subfields))
     :lines (vec (map-indexed (fn [index line]
                                (if (even? index)
                                  {:line (:line line)
                                   :line-from (:adjusted-from line)
                                   :line-data [line]}
                                  {:line (:line line)
                                   :line-from (:adjusted-to line)
                                   :line-data [line]}))
                              lines))}))
