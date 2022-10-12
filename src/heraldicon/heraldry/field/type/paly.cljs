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
   [heraldicon.svg.shape :as shape]))

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
                                :max 16
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
  (let [{:keys [width points]} (interface/get-subfields-environment context)
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

(defmethod interface/subfield-render-shapes field-type [context {:keys [line edges]}]
  (let [{:keys [bounding-box]} (interface/get-subfields-environment context)
        lines (vec (map-indexed
                    (fn [index [edge-start edge-end]]
                      ;; first line isn't needed
                      (when (pos? index)
                        (if (even? index)
                          (line/create-with-extension context
                                                      line
                                                      edge-start edge-end
                                                      bounding-box
                                                      :reversed? true
                                                      :flipped? true
                                                      :mirrored? true)
                          (line/create-with-extension context
                                                      line
                                                      edge-start edge-end
                                                      bounding-box))))
                    edges))]
    {:subfields (into []
                      (comp
                       (map (fn [[[line-1 line-2] glue glue-shortest]]
                              (cond
                                (and line-1
                                     line-2) [line-1 glue-shortest
                                              line-2 glue-shortest]
                                line-1 [line-1 glue]
                                line-2 [line-2 glue]
                                :else [:full])))
                       (map-indexed (fn [idx shape-data]
                                      (apply shape/build-shape
                                             (c/++ context :fields idx)
                                             shape-data)))
                       (map (fn [path]
                              {:shape [path]})))
                      (map vector
                           (partition 2 1 [nil] lines)
                           (cycle [:clockwise :counter-clockwise])
                           (cycle [:clockwise-shortest :counter-clockwise-shortest])))
     :edges (mapv (fn [line]
                    {:lines [line]})
                  (drop 1 lines))}))
