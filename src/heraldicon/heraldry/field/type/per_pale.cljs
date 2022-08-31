(ns heraldicon.heraldry.field.type.per-pale
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

(def field-type :heraldry.field.type/per-pale)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-pale)

(defmethod field.interface/part-names field-type [_] ["dexter" "sinister"])

(defmethod field.interface/options field-type [context]
  {:anchor {:point {:type :option.type/choice
                    :choices (position/anchor-choices
                              [:fess
                               :dexter
                               :sinister
                               :hoist
                               :fly
                               :left
                               :right])
                    :default :fess
                    :ui/label :string.option/point}
            :offset-x {:type :option.type/range
                       :min -45
                       :max 45
                       :default 0
                       :ui/label :string.option/offset-x
                       :ui/step 0.1}
            :ui/label :string.option/anchor
            :ui/element :ui.element/position}
   :line (line/options (c/++ context :line))})

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-effective-environment context)
        {:keys [top bottom]} (:points parent-environment)
        percentage-base (:height parent-environment)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        edge-x (:x anchor-point)
        parent-shape (interface/get-exact-parent-shape context)
        [edge-top edge-bottom] (v/intersections-with-shape
                                (v/Vector. edge-x (:y top)) (v/Vector. edge-x (:y bottom))
                                parent-shape :default? true)
        line-length (- (:y edge-bottom) (:y edge-top))]
    (post-process/properties
     {:type field-type
      :edge [edge-top edge-bottom]
      :line-length line-length
      :percentage-base percentage-base
      :num-subfields 2}
     context)))

(defmethod interface/subfield-environments field-type [context {[edge-top edge-bottom] :edge}]
  (let [{:keys [points]} (interface/get-effective-environment context)
        {:keys [top-left top-right
                bottom-left bottom-right]} points]
    {:subfields [(environment/create (bb/from-points [top-left bottom-left
                                                      edge-top edge-bottom]))
                 (environment/create (bb/from-points [edge-top edge-bottom
                                                      top-right bottom-right]))]}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line]
                                                                 [edge-top edge-bottom] :edge}]
  (let [{:keys [bounding-box]} (interface/get-effective-environment context)
        line-edge (line/create-with-extension line
                                              edge-top edge-bottom
                                              bounding-box
                                              :context context)]
    {:subfields [{:shape [(shape/build-shape
                           context
                           line-edge
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge
                           :counter-clockwise)]}]
     :lines [{:segments [line-edge]}]}))
