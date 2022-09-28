(ns heraldicon.heraldry.field.type.vairy
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/vairy)

(defmethod field.interface/display-name field-type [_] :string.field.type/vairy)

(defmethod field.interface/part-names field-type [_] nil)

;; TODO: needs translation
(def ^:private variant-choices
  [[:string.option.variant-vairy-choice/default :default]
   [:string.option.variant-vairy-choice/counter :counter]
   [:string.option.variant-vairy-choice/in-pale :in-pale]
   [:string.option.variant-vairy-choice/en-point :en-point]
   [:string.option.variant-vairy-choice/ancien :ancien]])

(def variant-map
  (options/choices->map variant-choices))

(defmethod field.interface/options field-type [_context]
  {:variant {:type :option.type/choice
             :choices variant-choices
             :default :default
             :ui/label :string.option/variant}
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
                       :min -180
                       :max 180
                       :default 0
                       :ui/label :string.option/rotation
                       :ui/step 0.01}
            :ui/label :string.option/layout
            :ui/element :ui.element/field-layout}})

(def ^:private sqr2 1.4142135623730951)

(defn- vair-default [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               "z")}]
                               ;; (1, 1) extra to prevent anti-aliasing
               [:path {:d (str "M -1," (dec middle-y)
                               "L 0," middle-y
                               "l" w "," h
                               "v" (* sqr2 h)
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L" (inc middle-x) "," (inc height)
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L -1," (inc height)
                               "z")}]
                               ;; (1, 1) extra to prevent anti-aliasing
               [:path {:d (str "M " (inc width) "," (dec middle-y)
                               "L" width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L" (dec middle-x) "," (inc height)
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L " (inc width) "," (inc height)
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y)}]
               [:path {:d (str "M 0," middle-y
                               "h" width)}]
               [:path {:d (str "M 0," middle-y
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "L 0," height)}]
               [:path {:d (str "M " width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "L " width "," height)}]]}))

(defn- vair-counter [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "l" (- w) "," (- h)
                               "v" (* sqr2 (- h))
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L" middle-x "," height
                               "l" (- w) "," (- h)
                               "v" (* sqr2 (- h))
                               "z")}]]}))

(defn- vair-in-pale [part-width part-height]
  (let [width part-width
        height part-height
        middle-x (/ width 2)
        w (/ width 4)
        h (/ height (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
                               ;; (1, 1) extra to prevent anti-aliasing
               [:path {:d (str "M -1," (inc height)
                               "L 0," height
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," height
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L" (inc width) "," (inc height)
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," height)}]
               [:path {:d (str "M 0," height
                               "h" width)}]]}))

(defn- vair-en-point [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y (+ 1 1 sqr2))]
    {:width width
     :height height
     :pattern [:<>
                               ;; (1, 1) extra to prevent anti-aliasing
               [:path {:d (str "M -1," (inc middle-y)
                               "L 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L" (inc width) "," (inc middle-y)
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L" (inc width) "," (inc height)
                               "L" width "," height
                               "l" (- w) "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L 0," height
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L -1," (inc height)
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "l" w "," (- h)
                               "v" (* sqr2 (- h))
                               "L" middle-x ",0"
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," middle-y)}]
               [:path {:d (str "M " middle-x "," middle-y
                               "l" w "," h
                               "v" (* sqr2 h)
                               "L" width "," height)}]
               [:path {:d (str "M " middle-x "," middle-y
                               "l" (- w) "," h
                               "v" (* sqr2 h)
                               "L 0," height)}]]}))

(defn- vair-ancien [part-width part-height]
  (let [width part-width
        height part-height
        dy 0.333
        w (/ width 4)
        h (/ height (+ 1 1 1 (* 2 dy)))]
    {:width width
     :height height
     :pattern [:<>
                               ;; (1, 1) extra to prevent anti-aliasing
               [:path {:d (str "M -1,-1"
                               ;; (1, 1) extra to prevent anti-aliasing
                               "L" (inc width) ",-1"
                               ;; (1, 0) extra to prevent anti-aliasing
                               "L" (inc width) "," (- height (* dy h))
                               "L" width "," (- height (* dy h))
                               "a" w " " h " 0 0 1 " (- w) "," (- h)
                               "v" (- h)
                               "a" w " " h " 0 0 0 " (- (* 2 w)) ",0"
                               "v" h
                               "a" w " " h " 0 0 1 " (- w) "," h
                               ;; (1, 0) extra to prevent anti-aliasing
                               "L -1," (- height (* dy h))
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M" width "," (- height (* dy h))
                               "a" w " " h " 0 0 1 " (- w) "," (- h)
                               "v" (- h)
                               "a" w " " h " 0 0 0 " (- (* 2 w)) ",0"
                               "v" h
                               "a" w " " h " 0 0 1 " (- w) "," h)}]
               [:path {:d (str "M 0," height
                               "h" width)}]]}))

(defn- render [context {:keys [start part-width part-height rotation]}]
  (let [variant (interface/get-sanitized-data (c/++ context :variant))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        pattern-id (uid/generate "vairy")
        vair-function (case variant
                        :counter vair-counter
                        :in-pale vair-in-pale
                        :en-point vair-en-point
                        :ancien vair-ancien
                        vair-default)
        {pattern-width :width
         pattern-height :height
         vair-pattern :pattern
         vair-outline :outline} (vair-function part-width part-height)]
    [:g {:transform (str "rotate(" (- rotation) ")")}
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
                   :width pattern-width
                   :height pattern-height
                   :x (:x start)
                   :y (:y start)
                   :pattern-units "userSpaceOnUse"}
         [:g (outline/style context)
          vair-outline]])
      (into [:<>]
            (map (fn [idx]
                   ^{:key idx}
                   [:pattern {:id (str pattern-id "-" idx)
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
                            :fill (get ["#000000" "#ffffff"] idx)}]
                    [:g {:fill (get ["#ffffff" "#000000"] idx)}
                     vair-pattern]]))
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
                              :fill (str "url(#" pattern-id "-" idx ")")}]]
                     [tincture/tinctured-field
                      (c/++ context :fields idx :field)
                      :mask-id mask-id]])))
           (range 2))
     (when outline?
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id "-outline)")}])]))

(defmethod interface/properties field-type [context]
  (let [{:keys [width height points]} (interface/get-effective-parent-environment context)
        {:keys [top-left]} points
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        rotation (interface/get-sanitized-data (c/++ context :layout :rotation))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        unstretched-part-height (if raw-num-fields-y
                                  (/ height num-fields-y)
                                  (-> part-width
                                      (/ 4)
                                      (* (+ 1 1 sqr2))))
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
     :render-fn render}))

(defmethod interface/subfield-environments field-type [_context _properties]
  nil)

(defmethod interface/subfield-render-shapes field-type [_context _properties]
  nil)
