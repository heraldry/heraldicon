(ns heraldicon.heraldry.field.type.masony
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/masony)

(defmethod field.interface/display-name field-type [_] :string.field.type/masony)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [_context]
  {:thickness {:type :option.type/range
               :min 0
               :max 0.5
               :default 0.1
               :ui/label :string.option/thickness
               :ui/step 0.01}
   :layout {:num-fields-x {:type :option.type/range
                           :min 1
                           :max 20
                           :default 6
                           :ui/label :string.option/subfields-x
                           :ui/element :ui.element/field-layout-num-fields-x}
            :num-fields-y {:type :option.type/range
                           :min 1
                           :max 20
                           :default 6
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
                       :min -90
                       :max 90
                       :default 0
                       :ui/label :string.option/rotation
                       :ui/step 0.01}
            :ui/label :string.option/layout
            :ui/element :ui.element/field-layout}})

(defn- masony-default [part-width part-height thickness]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        dx (-> thickness
               (* width)
               (/ 2))]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M" dx "," dx
                               "V" (- middle-y dx)
                               "H" (- width dx)
                               "V" dx
                               "z")}]
                               ;; (1, 0) extra to prevent anti-aliasing
               [:path {:d (str "M" -1 "," (+ middle-y dx)
                               "H" (- middle-x dx)
                               "V" (- height dx)
                               ;; (1, 0) extra to prevent anti-aliasing
                               "H" -1
                               "z")}]
                               ;; (1, 0) extra to prevent anti-aliasing
               [:path {:d (str "M" (inc width) "," (+ middle-y dx)
                               "H" (+ middle-x dx)
                               "V" (- height dx)
                               ;; (1, 0) extra to prevent anti-aliasing
                               "H" (inc width)
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M" dx "," dx
                               "V" (- middle-y dx)
                               "H" (- width dx)
                               "V" dx
                               "z")}]
               [:path {:d (str "M 0," (+ middle-y dx)
                               "H" (- middle-x dx)
                               "V" (- height dx)
                               "H 0")}]
               [:path {:d (str "M" width "," (+ middle-y dx)
                               "H" (+ middle-x dx)
                               "V" (- height dx)
                               "H" width)}]]}))

(defn- render [context {:keys [start part-width part-height rotation thickness]}]
  (let [outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        pattern-id (uid/generate "masony")
        {pattern-width :width
         pattern-height :height
         masony-pattern :pattern
         masony-outline :outline} (masony-default part-width part-height thickness)]
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
          masony-outline]])
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
                     masony-pattern]]))
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
  (let [{:keys [width height points]} (interface/get-subfields-environment context)
        {:keys [top-left]} points
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        rotation (interface/get-sanitized-data (c/++ context :layout :rotation))
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        unstretched-part-height (if raw-num-fields-y
                                  (/ height num-fields-y)
                                  (/ part-width 2))
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
     :render-fn render}))

(defmethod interface/subfield-environments field-type [_context]
  nil)

(defmethod interface/subfield-render-shapes field-type [_context]
  nil)
