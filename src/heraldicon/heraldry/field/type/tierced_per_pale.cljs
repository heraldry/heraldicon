(ns heraldicon.heraldry.field.type.tierced-per-pale
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

(def field-type :heraldry.field.type/tierced-per-pale)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-pale)

(defmethod field.interface/part-names field-type [_] ["dexter" "fess" "sinister"])

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
                                 :dexter
                                 :sinister
                                 :hoist
                                 :fly
                                 :left
                                 :center
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
     :layout {:stretch-x {:type :option.type/range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch-x
                          :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style
     :opposite-line opposite-line-style}))

(defmethod interface/properties field-type [context]
  (let [{:keys [width]
         :as parent-environment} (interface/get-subfields-environment context)
        {:keys [top bottom]} (:points parent-environment)
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        middle-width (-> width
                         (/ 3)
                         (* stretch-x))
        edge-1-x (- (:x anchor-point) (/ middle-width 2))
        edge-2-x (+ edge-1-x middle-width)
        parent-shape (interface/get-subfields-shape context)
        [edge-1-top edge-1-bottom] (v/intersections-with-shape
                                    (v/Vector. edge-1-x (:y top)) (v/Vector. edge-1-x (:y bottom))
                                    parent-shape :default? true)
        [edge-2-top edge-2-bottom] (v/intersections-with-shape
                                    (v/Vector. edge-2-x (:y top)) (v/Vector. edge-2-x (:y bottom))
                                    parent-shape :default? true)
        start-y (min (:y edge-1-top) (:y edge-2-top))
        edge-1-top (assoc edge-1-top :y start-y)
        edge-2-top (assoc edge-2-top :y start-y)
        line-length (max (v/abs (v/sub edge-1-top edge-1-bottom))
                         (v/abs (v/sub edge-2-top edge-2-bottom)))]
    (post-process/properties
     {:type field-type
      :edge-1 [edge-1-top edge-1-bottom]
      :edge-2 [edge-2-top edge-2-bottom]
      :line-length line-length
      :percentage-base width
      :num-subfields 3}
     context)))

(defmethod interface/subfield-environments field-type [context]
  (let [{[edge-1-top edge-1-bottom] :edge-1
         [edge-2-top edge-2-bottom] :edge-2} (interface/get-properties context)
        {:keys [points]} (interface/get-subfields-environment context)
        {:keys [top-left top-right]} points]
    {:subfields [(environment/create (bb/from-points [top-left edge-1-top edge-1-bottom]))
                 (environment/create (bb/from-points [edge-1-top edge-1-bottom
                                                      edge-2-top edge-2-bottom]))
                 (environment/create (bb/from-points [top-right edge-2-top edge-2-bottom]))]}))

(defmethod interface/subfield-render-shapes field-type [context]
  (let [{:keys [line opposite-line]
         [edge-1-top edge-1-bottom] :edge-1
         [edge-2-top edge-2-bottom] :edge-2} (interface/get-properties context)
        {:keys [bounding-box]} (interface/get-subfields-environment context)
        line-edge-1 (line/create-with-extension context
                                                line
                                                edge-1-top edge-1-bottom
                                                bounding-box)
        line-edge-2 (line/create-with-extension context
                                                opposite-line
                                                edge-2-top edge-2-bottom
                                                bounding-box
                                                :reversed? true
                                                :flipped? true
                                                :mirrored? true)]
    {:subfields [{:shape [(shape/build-shape
                           (c/++ context :fields 0)
                           line-edge-1
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 1)
                           line-edge-1
                           :counter-clockwise-shortest
                           line-edge-2
                           :counter-clockwise-shortest)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 1)
                           line-edge-2
                           :clockwise)]}]
     :edges [{:lines [line-edge-1]}
             {:lines [line-edge-2]}]}))
