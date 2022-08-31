(ns heraldicon.heraldry.field.type.per-bend
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

(def field-type :heraldry.field.type/per-bend)

(defmethod field.interface/display-name field-type [_] :string.field.type/per-bend)

(defmethod field.interface/part-names field-type [_] ["chief" "base"])

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line))
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
                                       [:fess
                                        :chief
                                        :base
                                        :honour
                                        :nombril
                                        :hoist
                                        :fly
                                        :top-left
                                        :center
                                        :bottom-right])
                             :default :top-left
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-anchor-point
                                              :top-left [:fess
                                                         :chief
                                                         :base
                                                         :honour
                                                         :nombril
                                                         :hoist
                                                         :fly
                                                         :bottom-right
                                                         :center
                                                         :angle]
                                              :bottom-right [:fess
                                                             :chief
                                                             :base
                                                             :honour
                                                             :nombril
                                                             :hoist
                                                             :fly
                                                             :top-left
                                                             :center
                                                             :angle]
                                              [:top-left
                                               :bottom-right
                                               :angle]))
                                  :default (case current-anchor-point
                                             :top-left :fess
                                             :bottom-right :fess
                                             :top-left)
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point anchor-point-option
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
     :orientation (cond-> {:point orientation-point-option
                           :ui/label :string.option/orientation
                           :ui/element :ui.element/position}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :option.type/range
                                              :min 0
                                              :max 360
                                              :default 45
                                              :ui/label :string.option/angle})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :option.type/range
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
                                                    :ui/step 0.1}))
     :line line-style}))

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-effective-environment context)
        sinister? (= (interface/get-raw-data (c/++ context :type))
                     :heraldry.field.type/per-bend-sinister)
        percentage-base (:height parent-environment)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               0
                                               nil)
        direction (v/sub orientation-point anchor-point)
        direction (-> (v/Vector. (-> direction :x Math/abs)
                                 (-> direction :y Math/abs))
                      v/normal
                      (cond->
                        sinister? (v/dot (v/Vector. 1 -1))))
        parent-shape (interface/get-exact-parent-shape context)
        [edge-start edge-end] (v/intersections-with-shape anchor-point (v/add anchor-point direction)
                                                          parent-shape :default? true)
        line-length (v/abs (v/sub edge-start edge-end))]
    (post-process/properties
     {:type field-type
      :sinister? sinister?
      :edge [edge-start edge-end]
      :line-length line-length
      :percentage-base percentage-base
      :num-subfields 2}
     context)))

(defmethod interface/subfield-environments field-type [context {:keys [sinister?]
                                                                [edge-start edge-end] :edge}]
  (let [{:keys [points]} (interface/get-effective-environment context)
        {:keys [top-left top-right
                bottom-left bottom-right]} points]
    {:subfields [(let [points [(if sinister?
                                 top-left
                                 top-right)
                               edge-start edge-end]]
                   (environment/create (bb/from-points points) {:fess (apply v/avg points)}))
                 (let [points [(if sinister?
                                 bottom-right
                                 bottom-left)
                               edge-start edge-end]]
                   (environment/create (bb/from-points points) {:fess (apply v/avg points)}))]}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line]
                                                                 [edge-start edge-end] :edge}]
  (let [{:keys [bounding-box]} (interface/get-effective-environment context)
        line-edge (line/create-with-extension line
                                              edge-start edge-end
                                              bounding-box
                                              :context context)]
    {:subfields [{:shape [(shape/build-shape
                           context
                           line-edge
                           :counter-clockwise)]}
                 {:shape [(shape/build-shape
                           context
                           line-edge
                           :clockwise)]}]
     :lines [{:segments [line-edge]}]}))
