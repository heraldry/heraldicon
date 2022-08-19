(ns heraldicon.heraldry.field.type.barry
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/barry)

(defmethod field.interface/display-name field-type [_] :string.field.type/barry)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  {:layout {:num-fields-y {:type :option.type/range
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
                       :min -1
                       :max 1
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
   :line (line/options (c/++ context :line)
                       :fimbriation? false)})

(defn barry-parts [top-left bottom-right line outline? context]
  (let [environment (:environment context)
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        height (- (:y bottom-right)
                  (:y top-left))
        bar-height (-> height
                       (/ num-fields-y)
                       (* stretch-y))
        required-height (* bar-height
                           num-fields-y)
        middle (-> height
                   (/ 2)
                   (+ (:y top-left)))
        y0 (-> middle
               (- (/ required-height 2))
               (+ (* offset-y
                     bar-height)))
        x1 (:x top-left)
        x2 (:x bottom-right)
        width (- x2 x1)
        {line-right :line
         line-right-start :line-start
         line-right-end :line-end} (line/create line
                                                top-left
                                                (v/add top-left (v/Vector. width 0))
                                                :real-start 0
                                                :real-end width
                                                :context context
                                                :environment environment)
        {line-left :line
         line-left-start :line-start
         line-left-end :line-end} (line/create line
                                               top-left
                                               (v/add top-left (v/Vector. width 0))
                                               :flipped? true
                                               :mirrored? true
                                               :reversed? true
                                               :real-start 0
                                               :real-end width
                                               :context context
                                               :environment environment)
        parts (->> (range num-fields-y)
                   (map (fn [i]
                          (let [y1 (+ y0 (* i bar-height))
                                y2 (+ y1 bar-height)
                                last-part? (-> i inc (= num-fields-y))
                                line-one-left (v/Vector. x1 y1)
                                line-one-right (v/Vector. x2 y1)
                                line-two-left (v/Vector. x1 y2)
                                line-two-right (v/Vector. x2 y2)]
                            [(cond
                               (and (zero? i)
                                    last-part?) ["M" -1000 -1000
                                                 "h" 2000
                                                 "v" 2000
                                                 "h" -2000
                                                 "z"]
                               (zero? i) ["M" (v/add line-two-left
                                                     line-right-start)
                                          (path/stitch line-right)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [(v/add line-two-right
                                                                 line-right-end)
                                                          (v/add line-two-left
                                                                 line-right-start)])
                                          "z"]
                               (even? i) (concat ["M" (v/add line-one-right
                                                             line-left-start)
                                                  (path/stitch line-left)]
                                                 (cond
                                                   last-part? [(infinity/path :counter-clockwise
                                                                              [:left :right]
                                                                              [(v/add line-one-left
                                                                                      line-left-end)
                                                                               (v/add line-one-right
                                                                                      line-left-start)])
                                                               "z"]
                                                   :else [(infinity/path :counter-clockwise
                                                                         [:left :left]
                                                                         [(v/add line-one-left
                                                                                 line-left-end)
                                                                          (v/add line-two-left
                                                                                 line-right-start)])
                                                          (path/stitch line-right)
                                                          (infinity/path :counter-clockwise
                                                                         [:right :right]
                                                                         [(v/add line-two-right
                                                                                 line-right-end)
                                                                          (v/add line-one-right
                                                                                 line-left-start)])]))
                               :else (concat ["M" (v/add line-one-left
                                                         line-right-start)
                                              (path/stitch line-right)]
                                             (cond
                                               last-part? [(infinity/path :clockwise
                                                                          [:right :left]
                                                                          [(v/add line-one-right
                                                                                  line-right-end)
                                                                           (v/add line-one-left
                                                                                  line-right-start)])
                                                           "z"]
                                               :else [(infinity/path :clockwise
                                                                     [:right :right]
                                                                     [(v/add line-one-right
                                                                             line-right-end)
                                                                      (v/add line-two-right
                                                                             line-left-start)])
                                                      (path/stitch line-left)
                                                      (infinity/path :clockwise
                                                                     [:left :left]
                                                                     [(v/add line-two-left
                                                                             line-left-end)
                                                                      (v/add line-one-left
                                                                             line-right-start)])
                                                      "z"])))
                             [line-one-left line-two-right]])))
                   vec)
        edges (->> num-fields-y
                   dec
                   range
                   (map (fn [i]
                          (let [y1 (+ y0 (* i bar-height))
                                y2 (+ y1 bar-height)
                                line-two-left (v/Vector. x1 y2)
                                line-two-right (v/Vector. x2 y2)]
                            (if (even? i)
                              (path/make-path ["M" (v/add line-two-left
                                                          line-right-start)
                                               (path/stitch line-right)])
                              (path/make-path ["M" (v/add line-two-right
                                                          line-left-start)
                                               (path/stitch line-left)])))))
                   vec)
        overlap (-> edges
                    (->> (map vector))
                    vec
                    (conj nil))
        outlines (when outline?
                   (into [:g (outline/style context)]
                         (map (fn [i]
                                ^{:key i}
                                [:path {:d (nth edges i)}]))
                         (range (dec num-fields-y))))]
    [parts overlap outlines]))

(defmethod interface/properties field-type [context]
  (let [{:keys [height points]} (interface/get-parent-environment context)
        {:keys [center left right]} points
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        part-height (-> height
                        (/ num-fields-y)
                        (* stretch-y))
        required-height (* part-height
                           num-fields-y)
        y0 (-> (:y center)
               (- (/ required-height 2))
               (+ (* offset-y
                     part-height)))
        parent-shape (interface/get-exact-parent-shape context)
        edges (mapv (fn [i]
                      (let [edge-y (+ y0 (* i part-height))]
                        (v/intersections-with-shape
                         (v/Vector. (:x left) edge-y) (v/Vector. (:x right) edge-y)
                         parent-shape :default? true)))
                    (range num-fields-y))
        ;; the second edge is the first one visible, it dictates the line style start
        start-x (-> edges second first :x)
        edges (mapv (fn [[edge-start edge-end]]
                      [(assoc edge-start :x start-x) edge-end])
                    edges)
        max-x (apply max (map (comp :x second) edges))
        line-length (apply max (map (fn [[edge-start edge-end]]
                                      (v/abs (v/sub edge-end edge-start)))
                                    edges))]
    (post-process/properties
     {:type field-type
      :edges edges
      :part-height part-height
      :start-x start-x
      :max-x max-x
      :line-length line-length
      :num-subfields num-fields-y}
     context)))

(defmethod interface/subfield-environments field-type [context {:keys [edges start-x max-x part-height]}]
  (let [{:keys [meta]} (interface/get-parent-environment context)]
    {:subfields (into []
                      (map (fn [[edge-start _edge-end]]
                             (environment/create
                              {:paths nil}
                              (-> meta
                                  (dissoc :context)
                                  (assoc :bounding-box (bb/from-points [(assoc edge-start :x start-x)
                                                                        (assoc edge-start
                                                                               :x max-x
                                                                               :y (+ (:y edge-start) part-height))]))))))
                      edges)}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line edges num-subfields]}]
  (let [{:keys [meta points]} (interface/get-parent-environment context)
        {:keys [top-left bottom-right]} points
        bounding-box (:bounding-box meta)
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
                                               (infinity/clockwise outside-1 outside-2)
                                               (infinity/clockwise outside-2 outside-1)
                                               "z"]
                                  first? (let [line-1 (get lines i)
                                               line-start (v/add (:adjusted-from line-1) (:line-start line-1))
                                               line-end (:adjusted-to line-1)]
                                           ["M" line-start
                                            (path/stitch (:line line-1))
                                            (infinity/counter-clockwise line-end line-start)
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
                                              infinity/clockwise
                                              infinity/counter-clockwise) line-end line-start)
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
                                               (infinity/counter-clockwise line-1-end line-2-start)
                                               (path/stitch (:line line-2))
                                               (infinity/counter-clockwise line-2-end line-1-start)
                                               "z"])
                                            (let [line-1-start (v/add (:adjusted-from line-1) (:line-start line-1))
                                                  line-1-end (:adjusted-to line-1)
                                                  line-2-start (v/add (:adjusted-to line-2) (:line-start line-2))
                                                  line-2-end (:adjusted-from line-2)]

                                              ["M" line-1-start
                                               (path/stitch (:line line-1))
                                               (infinity/clockwise line-1-end line-2-start)
                                               (path/stitch (:line line-2))
                                               (infinity/clockwise line-2-end line-1-start)
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
