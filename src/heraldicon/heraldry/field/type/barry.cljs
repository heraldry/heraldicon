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
   [heraldicon.svg.shape :as shape]))

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
                              :max 16
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

(defmethod interface/properties field-type [context]
  (let [{:keys [height points]} (interface/get-subfields-environment context)
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
        parent-shape (interface/get-subfields-shape context)
        edges (mapv (fn [i]
                      (let [edge-y (+ y0 (* i part-height))]
                        (v/intersections-with-shape
                         (v/Vector. (:x left) edge-y) (v/Vector. (:x right) edge-y)
                         parent-shape :default? true)))
                    (range num-fields-y))
        ;; the second edge is the first one visible, so drop the first
        start-x (->> (drop 1 edges)
                     (map (comp :x first))
                     (apply min))
        end-x (apply max (map (comp :x second) edges))
        edges (mapv (fn [[edge-start edge-end]]
                      [(assoc edge-start :x start-x)
                       (assoc edge-end :x end-x)])
                    edges)
        line-length (apply max (map (fn [[edge-start edge-end]]
                                      (v/abs (v/sub edge-end edge-start)))
                                    edges))]
    (post-process/properties
     {:type field-type
      :edges edges
      :part-height part-height
      :start-x start-x
      :end-x end-x
      :line-length line-length
      :num-subfields num-fields-y}
     context)))

(defmethod interface/subfield-environments field-type [_context {:keys [edges start-x end-x part-height]}]
  {:subfields (mapv (fn [[edge-start _edge-end]]
                      (environment/create (bb/from-points [(assoc edge-start :x start-x)
                                                           (assoc edge-start
                                                                  :x end-x
                                                                  :y (+ (:y edge-start) part-height))])))
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
                           (cycle [:counter-clockwise :clockwise])
                           (cycle [:counter-clockwise-shortest :clockwise-shortest])))
     :edges (mapv (fn [line]
                    {:lines [line]})
                  (drop 1 lines))}))
