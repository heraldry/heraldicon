(ns heraldicon.heraldry.field.type.fretty
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/fretty)

(defmethod field.interface/display-name field-type [_] :string.field.type/fretty)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [_context]
  {:thickness {:type :option.type/range
               :min 0
               :max 0.5
               :default 0.1
               :ui/label :string.option/thickness
               :ui/step 0.01}
   :gap {:type :option.type/range
         :min 0
         :max 1
         :default 0.1
         :ui/label :string.option/gap
         :ui/step 0.01}
   :layout {:num-fields-x {:type :option.type/range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui/label :string.option/subfields-x
                           :ui/element :ui.element/field-layout-num-fields-x}
            :num-fields-y {:type :option.type/range
                           :min 1
                           :max 20
                           :default 6
                           :integer? true
                           :ui/label :string.option/subfields-y
                           :ui/element :ui.element/field-layout-num-fields-y}
            :offset-x {:type :option.type/range
                       :min -1
                       :max 1
                       :default 0
                       :ui/label :string.option/offset-x
                       :ui/step 0.01}
            :offset-y {:type :option.type/range
                       :min -1
                       :max 1
                       :default 0
                       :ui/label :string.option/offset-y
                       :ui/step 0.01}
            :stretch-x {:type :option.type/range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui/label :string.option/stretch-x
                        :ui/step 0.01}
            :stretch-y {:type :option.type/range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui/label :string.option/stretch-y
                        :ui/step 0.01}
            :rotation {:type :option.type/range
                       :min -45
                       :max 45
                       :default 0
                       :ui/label :string.option/rotation
                       :ui/step 0.01}
            :ui/label :string.option/layout
            :ui/element :ui.element/field-layout}})

(defn- fretty-default [part-width part-height thickness gap]
  (let [width (* 2 part-width)
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        thickness (* thickness width)
        gap (* gap width)
        half-thickness (/ thickness 2)]
    {:width width
     :height height
     :pattern [:<>
               ;; -|-
                               ;; (1, 1) extra to prevent anti-aliasing
               [:path {:d (str "M -1,-1"
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" 1
                               "h" (- middle-x half-thickness gap)
                               ;; (0, 1) extra to prevent anti-aliasing
                               "v" 1
                               "v" half-thickness
                               "h" (- (- middle-x half-thickness gap))
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" -1
                               "z")}]
                               ;; (0, 1) extra to prevent anti-aliasing
               [:path {:d (str "M" (- middle-x half-thickness) "," -1
                               "h " thickness
                               ;; (0, 1) extra to prevent anti-aliasing
                               "v" 1
                               "v" (- middle-y half-thickness gap)
                               "h" (- thickness)
                               "z")}]
                               ;; (0, 1) extra to prevent anti-aliasing
               [:path {:d (str "M" (+ middle-x half-thickness gap) ",-1"
                               "H" width
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" 1
                               ;; (0, 1) extra to prevent anti-aliasing
                               "v" 1
                               "v" half-thickness
                               ;; (0, 1) extra to prevent anti-aliasing
                               "h" -1
                               "h" (- (- middle-x half-thickness gap))
                               "z")}]

               ;; |-|
                               ;; (1, 0) extra to prevent anti-aliasing
               [:path {:d (str "M -1," (+ half-thickness gap)
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" 1
                               "h" half-thickness
                               "v" (- height thickness gap gap)
                               "h" (- half-thickness)
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" -1
                               "z")}]
               [:path {:d (str "M" (+ half-thickness gap) "," (- middle-y half-thickness)
                               "h" (- width thickness gap gap)
                               "v" thickness
                               "h" (- (- width thickness gap gap))
                               "z")}]
                               ;; (1, 0) extra to prevent anti-aliasing
               [:path {:d (str "M" (inc width) "," (+ half-thickness gap)
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" -1
                               "h" (- half-thickness)
                               "v" (- height thickness gap gap)
                               "h" half-thickness
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" 1
                               "z")}]

               ;; -|-
                               ;; (1, 1) extra to prevent anti-aliasing
               [:path {:d (str "M -1," (inc height)
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" 1
                               "h" (- middle-x half-thickness gap)
                               ;; (0, 1) extra to prevent anti-aliasing
                               "v" -1
                               "v" (- half-thickness)
                               "h" (- (- middle-x half-thickness gap))
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" -1
                               "z")}]
                               ;; (0, 1) extra to prevent anti-aliasing
               [:path {:d (str "M" (- middle-x half-thickness) "," (inc height)
                               "h" thickness
                               ;; (0, 1) extra to prevent anti-aliasing
                               "v" -1
                               "v" (- (- middle-y half-thickness gap))
                               "h" (- thickness)
                               "z")}]
                               ;; (0, 1) extra to prevent anti-aliasing
               [:path {:d (str "M" (+ middle-x half-thickness gap) "," (inc height)
                               "h" (- middle-x half-thickness gap)
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" 1
                               "v" (- half-thickness)
                               ;; (0, 1) extra to prevent anti-aliasing
                               "v" -1
                               ;; (1, 0) extra to prevent anti-aliasing
                               "h" -1
                               "h" (- (- middle-x half-thickness gap))
                               "z")}]]

     :outline [:<>
               ;; -|-
               [:path {:d (str "M" (- middle-x half-thickness gap) ",0"
                               "v" half-thickness
                               "h" (- (- middle-x half-thickness gap)))}]
               [:path {:d (str "M" (- middle-x half-thickness) "," 0
                               "v" (- middle-y half-thickness gap)
                               "h" thickness
                               "v" (- (- middle-y half-thickness gap)))}]
               [:path {:d (str "M" (+ middle-x half-thickness gap) ",0"
                               "v" half-thickness
                               "h" (- middle-x half-thickness gap))}]

               ;; |-|

               [:path {:d (str "M 0," (+ half-thickness gap)
                               "h" half-thickness
                               "v" (- height thickness gap gap)
                               "h" (- half-thickness))}]
               [:path {:d (str "M" (+ half-thickness gap) "," (- middle-y half-thickness)
                               "h" (- width thickness gap gap)
                               "v" thickness
                               "h" (- (- width thickness gap gap))
                               "z")}]
               [:path {:d (str "M" width "," (+ half-thickness gap)
                               "h" (- half-thickness)
                               "v" (- height thickness gap gap)
                               "h" half-thickness)}]

               ;; -|-

               [:path {:d (str "M" (- middle-x half-thickness gap) "," height
                               "v" (- half-thickness)
                               "h" (- (- middle-x half-thickness gap)))}]
               [:path {:d (str "M" (- middle-x half-thickness) "," height
                               "v" (- (- middle-y half-thickness gap))
                               "h" thickness
                               "v" (- middle-y half-thickness gap))}]
               [:path {:d (str "M" (+ middle-x half-thickness gap) "," height
                               "v" (- half-thickness)
                               "h" (- middle-x half-thickness gap))}]]}))

(defn- render [context {:keys [start part-width part-height rotation thickness gap]}]
  (let [outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        pattern-id-prefix (uid/generate "fretty")
        {pattern-width :width
         pattern-height :height
         fretty-pattern :pattern
         fretty-outline :outline} (fretty-default part-width part-height thickness gap)]
    [:g {:transform (str "rotate(" (- rotation) ")")}
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id-prefix "-outline")
                   :width pattern-width
                   :height pattern-height
                   :x (:x start)
                   :y (:y start)
                   :pattern-units "userSpaceOnUse"}
         [:g (outline/style context)
          fretty-outline]])
      (into [:<>]
            (map (fn [idx]
                   ^{:key idx}
                   [:pattern {:id (str pattern-id-prefix "-" idx)
                              :width pattern-width
                              :height pattern-height
                              :x (:x start)
                              :y (:y start)
                              :pattern-units "userSpaceOnUse"}
                    [:rect {:x -1
                            :y -1
                            :width (+ pattern-width 2)
                            :height (+ pattern-height 2)
                            :shape-rendering "crispEdges"
                            :fill (get ["#ffffff" "#000000"] idx)}]
                    [:g {:fill (get ["#000000" "#ffffff"] idx)}
                     fretty-pattern]]))
            (range 2))]
     (into [:<>]
           (map (fn [idx]
                  (let [mask-id (uid/generate "mask")]
                    ^{:key idx}
                    [:<>
                     [:mask {:id mask-id}
                      [:rect {:x -500
                              :y -500
                              :width 1100
                              :height 1100
                              :fill (str "url(#" pattern-id-prefix "-" idx ")")}]]
                     [tincture/tinctured-field
                      (c/++ context :fields idx :field)
                      :mask-id mask-id]])))
           (range 2))
     (when outline?
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id-prefix "-outline)")}])]))

(defmethod interface/properties field-type [context]
  (let [{:keys [width height points]} (interface/get-subfields-environment context)
        {:keys [top-left]} points
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        rotation (+ 45 (interface/get-sanitized-data (c/++ context :layout :rotation)))
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        gap (/ (interface/get-sanitized-data (c/++ context :gap))
               5)
        part-width (-> width
                       (/ num-fields-x)
                       (* 0.7071067811865476)
                       (* stretch-x))
        unstretched-part-height (if raw-num-fields-y
                                  (/ height num-fields-y)
                                  part-width)
        part-height (* unstretched-part-height stretch-y)
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        x0 (+ (:x top-left)
              (* part-width offset-x)
              shift-x)
        y0 (+ (:y top-left)
              (* part-height offset-y)
              shift-y)
        start (v/Vector. x0 y0)]
    {:type field-type
     :start start
     :num-fields-x num-fields-x
     :num-fields-y num-fields-y
     :part-width part-width
     :part-height part-height
     :rotation rotation
     :thickness thickness
     :gap gap
     :render-fn render}))

(defmethod interface/subfield-environments field-type [_context]
  nil)

(defmethod interface/subfield-render-shapes field-type [_context]
  nil)
