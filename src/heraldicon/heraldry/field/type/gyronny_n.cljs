(ns heraldicon.heraldry.field.type.gyronny-n
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def field-type :heraldry.field.type/gyronny-n)

(defmethod field.interface/display-name field-type [_] :string.field.type/gyronny-n)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))]
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
     :layout {:num-fields-x {:type :option.type/range
                             :min 3
                             :max 32
                             :default 6
                             :integer? true
                             :ui/label :string.option/subfields
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
                         :default -0.5
                         :ui/label :string.option/offset
                         :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style}))

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-effective-parent-environment context)
        num-fields (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        offset (interface/get-sanitized-data (c/++ context :layout :offset-x))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment)
        angle-step (/ 360 num-fields)
        start-angle (* offset angle-step)
        edge-angles (map #(-> %
                              (* angle-step)
                              (+ start-angle))
                         (range num-fields))
        parent-shape (interface/get-exact-parent-shape context)
        edge-ends (mapv (fn [angle]
                          (v/last-intersection-with-shape
                           anchor-point (v/rotate (v/Vector. 0 -1) angle)
                           parent-shape
                           :default? true
                           :relative? true))
                        edge-angles)
        line-length (->> edge-ends
                         (map #(-> %
                                   (v/sub anchor-point)
                                   v/abs))
                         (apply max))
        fess-points (mapv (fn [angle]
                            (-> (v/last-intersection-with-shape
                                 anchor-point (v/rotate (v/Vector. 0 (- line-length))
                                                        (+ angle
                                                           (/ angle-step 2)))
                                 parent-shape
                                 :default? true
                                 :relative? true)
                                (v/sub anchor-point)
                                (v/mul 0.6)
                                (v/add anchor-point)))
                          edge-angles)]
    (post-process/properties
     {:type field-type
      :line-length line-length
      :num-fields num-fields
      :fess-points fess-points
      :edge-start anchor-point
      :edge-ends edge-ends
      :num-subfields num-fields}
     context)))

(defmethod interface/subfield-environments field-type [_context {:keys [edge-start edge-ends
                                                                        fess-points num-fields]}]
  {:subfields (mapv
               (fn [index]
                 (environment/create (bb/from-points [(get edge-ends index)
                                                      edge-start
                                                      (get edge-ends (mod (inc index) num-fields))])
                                     {:fess (get fess-points index)}))
               (range num-fields))})

(defmethod interface/subfield-render-shapes field-type [context {:keys [edge-start edge-ends line num-fields]}]
  (let [{:keys [bounding-box]} (interface/get-effective-parent-environment context)
        lines (vec (map-indexed
                    (fn [index edge-end]
                      (if (even? index)
                        (line/create-with-extension context
                                                    line
                                                    edge-start edge-end
                                                    bounding-box
                                                    :reversed? true
                                                    :extend-from? false)
                        (line/create-with-extension context
                                                    line
                                                    edge-start edge-end
                                                    bounding-box
                                                    :flipped? true
                                                    :mirrored? true
                                                    :extend-from? false)))
                    edge-ends))]
    {:subfields (into []
                      (comp
                       (map (fn [[[line-1 line-2] glue]]
                              (let [last? (not line-2)
                                    line-2 (or line-2 (first lines))]
                                (if (and (odd? num-fields)
                                         last?)
                                  (if (= glue :counter-clockwise)
                                    [line-1 [:reverse line-2] glue]
                                    [line-2 [:reverse line-1] glue])
                                  (if (= glue :counter-clockwise)
                                    [line-1 line-2 glue]
                                    [line-2 line-1 glue])))))
                       (map #(apply shape/build-shape context %))
                       (map (fn [path]
                              {:shape [path]})))
                      (map vector
                           (partition 2 1 [nil] lines)
                           (cycle [:counter-clockwise :clockwise])))
     :edges (mapv (fn [line]
                    {:lines [line]}) lines)}))
