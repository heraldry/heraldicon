(ns heraldicon.heraldry.field.type.quartered
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

(def field-type :heraldry.field.type/quartered)

(defmethod field.interface/display-name field-type [_] :string.field.type/quartered)

(defmethod field.interface/part-names field-type [_] ["I" "II" "III" "IV"])

(defmethod field.interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
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
     :line line-style
     :opposite-line opposite-line-style
     :outline? options/plain-outline?-option}))

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-subfields-environment context)
        {:keys [top bottom left right]} (:points parent-environment)
        percentage-base (:height parent-environment)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        {edge-x :x
         edge-y :y} anchor-point
        parent-shape (interface/get-subfields-shape context)
        [left-end right-end] (v/intersections-with-shape
                              (v/Vector. (:x left) edge-y) (v/Vector. (:x right) edge-y)
                              parent-shape :default? true)
        [top-end bottom-end] (v/intersections-with-shape
                              (v/Vector. edge-x (:y top)) (v/Vector. edge-x (:y bottom))
                              parent-shape :default? true)
        line-length (->> [top-end bottom-end
                          left-end right-end]
                         (map (fn [v]
                                (v/sub v anchor-point)))
                         (map v/abs)
                         (apply max))]
    (post-process/properties
     {:type field-type
      :edge-left [anchor-point left-end]
      :edge-right [anchor-point right-end]
      :edge-top [anchor-point top-end]
      :edge-bottom [anchor-point bottom-end]
      :anchor-point anchor-point
      :line-length line-length
      :percentage-base percentage-base
      :num-subfields 4
      :overlap?-fn #{0 3}}
     context)))

(defmethod interface/subfield-environments field-type [context]
  (let [{:keys [anchor-point]
         [_edge-top-1 edge-top-2] :edge-top
         [_edge-bottom-1 edge-bottom-2] :edge-bottom
         [_edge-left-1 edge-left-2] :edge-left
         [_edge-right-1 edge-right-2] :edge-right} (interface/get-properties context)
        {:keys [points]} (interface/get-subfields-environment context)
        {:keys [top-left top-right
                bottom-left bottom-right]} points]
    {:subfields [(environment/create (bb/from-points [top-left edge-top-2
                                                      edge-left-2 anchor-point]))
                 (environment/create (bb/from-points [edge-top-2 top-right
                                                      anchor-point edge-right-2]))
                 (environment/create (bb/from-points [edge-left-2 anchor-point
                                                      bottom-left edge-bottom-2]))
                 (environment/create (bb/from-points [anchor-point edge-right-2
                                                      edge-bottom-2 bottom-right]))]}))

(defmethod interface/subfield-render-shapes field-type [context]
  (let [{:keys [line opposite-line]
         [edge-top-1 edge-top-2] :edge-top
         [edge-bottom-1 edge-bottom-2] :edge-bottom
         [edge-left-1 edge-left-2] :edge-left
         [edge-right-1 edge-right-2] :edge-right} (interface/get-properties context)
        {:keys [bounding-box]} (interface/get-subfields-environment context)
        line-edge-top (line/create-with-extension context
                                                  line
                                                  edge-top-1 edge-top-2
                                                  bounding-box
                                                  :reversed? true
                                                  :extend-from? false)
        line-edge-bottom (line/create-with-extension context
                                                     line
                                                     edge-bottom-1 edge-bottom-2
                                                     bounding-box
                                                     :reversed? true
                                                     :extend-from? false)
        line-edge-left (line/create-with-extension context
                                                   opposite-line
                                                   edge-left-1 edge-left-2
                                                   bounding-box
                                                   :mirrored? true
                                                   :flipped? true
                                                   :extend-from? false)
        line-edge-right (line/create-with-extension context
                                                    opposite-line
                                                    edge-right-1 edge-right-2
                                                    bounding-box
                                                    :mirrored? true
                                                    :flipped? true
                                                    :extend-from? false)]
    {:subfields [{:shape [(shape/build-shape
                           (c/++ context :fields 0)
                           line-edge-top
                           line-edge-left
                           :clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 1)
                           line-edge-top
                           line-edge-right
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 2)
                           line-edge-bottom
                           line-edge-left
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           (c/++ context :fields 3)
                           line-edge-bottom
                           line-edge-right
                           :clockwise)]}]
     :edges [{:lines [line-edge-top]}
             {:lines [line-edge-bottom]}
             {:lines [line-edge-left]}
             {:lines [line-edge-right]}]}))
