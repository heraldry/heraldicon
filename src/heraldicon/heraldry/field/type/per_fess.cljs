(ns heraldicon.heraldry.field.type.per-fess
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
   [heraldicon.svg.shape :as shape]))

(def field-type :heraldry.field.type/per-fess)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-fess)

(defmethod field.interface/part-names field-type [_] ["chief" "base"])

(defmethod field.interface/options field-type [context]
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
   :line (line/options (c/++ context :line))})

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-effective-parent-environment context)
        {:keys [left right]} (:points parent-environment)
        percentage-base (:height parent-environment)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        edge-y (:y anchor-point)
        parent-shape (interface/get-exact-parent-shape context)
        [edge-left edge-right] (v/intersections-with-shape
                                (v/Vector. (:x left) edge-y) (v/Vector. (:x right) edge-y)
                                parent-shape :default? true)
        line-length (- (:x edge-right) (:x edge-left))]
    (post-process/properties
     {:type field-type
      :edge [edge-left edge-right]
      :line-length line-length
      :percentage-base percentage-base
      :num-subfields 2}
     context)))

(defmethod interface/subfield-environments field-type [context {[edge-left edge-right] :edge}]
  (let [{:keys [points]} (interface/get-effective-parent-environment context)
        {:keys [top-left top-right
                bottom-left bottom-right]} points]
    {:subfields [(environment/create (bb/from-points [top-left top-right
                                                      edge-left edge-right]))
                 (environment/create (bb/from-points [edge-left edge-right
                                                      bottom-left bottom-right]))]}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line]
                                                                 [edge-left edge-right] :edge}]
  (let [{:keys [bounding-box]} (interface/get-effective-parent-environment context)
        line-edge (line/create-with-extension context
                                              line
                                              edge-left edge-right
                                              bounding-box)]
    {:subfields [{:shape [(shape/build-shape
                           context
                           line-edge
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge
                           :clockwise)]}]
     :edges [{:lines [line-edge]}]}))
