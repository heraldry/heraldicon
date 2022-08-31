(ns heraldicon.heraldry.field.type.bendy
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
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/bendy)

(defmethod field.interface/display-name field-type [_] :string.field.type/bendy)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)
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
     :layout {:num-fields-y {:type :option.type/range
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
     :line line-style}))

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        sinister? (= (interface/get-raw-data (c/++ context :type))
                     :heraldry.field.type/bendy-sinister)
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
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
        angle (v/angle-to-point v/zero direction)
        reverse-transform-fn (fn reverse-transform-fn [v]
                               (if (instance? v/Vector v)
                                 (v/rotate v (- angle))
                                 (path/rotate v (- angle))))
        parent-shape (interface/get-exact-parent-shape context)
        rotated-parent-shape (-> parent-shape
                                 path/parse-path
                                 reverse-transform-fn
                                 path/to-svg)
        bounding-box (bb/from-paths [rotated-parent-shape])
        available-height (bb/height bounding-box)
        part-height (-> available-height
                        (/ (or num-fields-y 1))
                        (* stretch-y))
        required-height (* part-height
                           num-fields-y)
        y0 (-> (:min-y bounding-box)
               (+ (/ available-height 2))
               (- (/ required-height 2))
               (+ (* offset-y
                     part-height)))
        edges (map (fn [i]
                     (let [edge-y (+ y0 (* i part-height))]
                       (v/intersections-with-shape
                        (v/Vector. (:min-x bounding-box) edge-y) (v/Vector. (:max-x bounding-box) edge-y)
                        rotated-parent-shape :default? true)))
                   (range num-fields-y))
        start-x (apply min (map (comp :x first) edges))
        end-x (apply max (map (comp :x second) edges))
        line-length (- end-x start-x)
        edges (map (fn [[edge-start edge-end]]
                     [(assoc edge-start :x start-x)
                      (assoc edge-end :x end-x)])
                   edges)
        edges (mapv (fn [[edge-start edge-end]]
                      [(v/rotate edge-start angle)
                       (v/rotate edge-end angle)])
                    edges)]
    (post-process/properties
     {:type field-type
      :edges edges
      :part-height part-height
      :start-x start-x
      :end-x end-x
      :line-length line-length
      :transform (str "rotate(" angle ")")
      :reverse-transform-fn reverse-transform-fn
      :num-subfields num-fields-y}
     context)))

(defmethod interface/subfield-environments field-type [_context {:keys [edges start-x end-x part-height
                                                                        reverse-transform-fn]}]
  {:subfields (mapv (fn [[edge-start _edge-end]]
                      (let [real-edge-start (reverse-transform-fn edge-start)]
                        (environment/create (bb/from-points [(assoc real-edge-start :x start-x)
                                                             (assoc real-edge-start
                                                                    :x end-x
                                                                    :y (+ (:y real-edge-start) part-height))]))))
                    edges)})

(defmethod interface/subfield-render-shapes field-type [context properties]
  ((get-method interface/subfield-render-shapes :heraldry.field.type/barry) context properties))
