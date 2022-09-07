(ns heraldicon.heraldry.field.type.tierced-per-fess
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

(def field-type :heraldry.field.type/tierced-per-fess)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-fess)

(defmethod field.interface/part-names field-type [_] ["chief" "fess" "base"])

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))]
    {:anchor {:point {:type :option.type/choice
                      :choices (position/anchor-choices
                                [:fess
                                 :chief
                                 :base
                                 :honour
                                 :nombril
                                 :top
                                 :center
                                 :bottom])
                      :default :fess
                      :ui/label :string.option/point}
              :offset-y {:type :option.type/range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-y
                         :ui/step 0.1}
              :ui/label :string.option/anchor
              :ui/element :ui.element/position}
     :layout {:stretch-y {:type :option.type/range
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
  (let [{:keys [height]
         :as parent-environment} (interface/get-effective-parent-environment context)
        {:keys [left right]} (:points parent-environment)
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        middle-height (-> height
                          (/ 3)
                          (* stretch-y))
        edge-1-y (- (:y anchor-point) (/ middle-height 2))
        edge-2-y (+ edge-1-y middle-height)
        parent-shape (interface/get-exact-parent-shape context)
        [edge-1-left edge-1-right] (v/intersections-with-shape
                                    (v/Vector. (:x left) edge-1-y) (v/Vector. (:x right) edge-1-y)
                                    parent-shape :default? true)
        [edge-2-left edge-2-right] (v/intersections-with-shape
                                    (v/Vector. (:x left) edge-2-y) (v/Vector. (:x right) edge-2-y)
                                    parent-shape :default? true)
        start-x (min (:x edge-1-left) (:x edge-2-left))
        edge-1-left (assoc edge-1-left :x start-x)
        edge-2-left (assoc edge-2-left :x start-x)
        line-length (max (v/abs (v/sub edge-1-left edge-1-right))
                         (v/abs (v/sub edge-2-left edge-2-right)))]
    (post-process/properties
     {:type field-type
      :edge-1 [edge-1-left edge-1-right]
      :edge-2 [edge-2-left edge-2-right]
      :line-length line-length
      :percentage-base height
      :num-subfields 3}
     context)))

(defmethod interface/subfield-environments field-type [context {[edge-1-left edge-1-right] :edge-1
                                                                [edge-2-left edge-2-right] :edge-2}]
  (let [{:keys [points]} (interface/get-effective-parent-environment context)
        {:keys [top-left top-right
                bottom-left bottom-right]} points]
    {:subfields [(environment/create (bb/from-points [top-left top-right edge-1-left edge-1-right]))
                 (environment/create (bb/from-points [edge-1-left edge-1-right
                                                      edge-2-left edge-2-right]))
                 (environment/create (bb/from-points [bottom-left bottom-right edge-2-left edge-2-right]))]}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line opposite-line]
                                                                 [edge-1-left edge-1-right] :edge-1
                                                                 [edge-2-left edge-2-right] :edge-2}]
  (let [{:keys [bounding-box]} (interface/get-effective-parent-environment context)
        line-edge-1 (line/create-with-extension context
                                                line
                                                edge-1-left edge-1-right
                                                bounding-box)
        line-edge-2 (line/create-with-extension context
                                                opposite-line
                                                edge-2-left edge-2-right
                                                bounding-box
                                                :reversed? true
                                                :flipped? true
                                                :mirrored? true)]
    {:subfields [{:shape [(shape/build-shape
                           context
                           line-edge-1
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge-1
                           :clockwise-shortest
                           line-edge-2
                           :clockwise-shortest)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge-2
                           :counter-clockwise)]}]
     :edges [{:lines [line-edge-1]}
             {:lines [line-edge-2]}]}))
