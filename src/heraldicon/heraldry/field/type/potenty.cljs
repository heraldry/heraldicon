(ns heraldicon.heraldry.field.type.potenty
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/potenty)

(defmethod field.interface/display-name field-type [_] :string.field.type/potenty)

(defmethod field.interface/part-names field-type [_] nil)

;; TODO: needs translation
(def ^:private variant-choices
  [["Default" :default]
   ["Counter" :counter]
   ["In pale" :in-pale]
   ["En point" :en-point]])

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

(defn- units [n]
  (dec (* n 4)))

(defn- potent-default [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M " middle-x "," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "H" width
                               "V" height
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," (- middle-y h)
                               "V" (+ middle-y h))}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]]}))

(defn- potent-counter [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," middle-y
                               "v" h
                               "h" w
                               "v" h
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," (- middle-y h)
                               "V" (+ middle-y h))}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]]}))

(defn- potent-in-pale [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0," height
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "h" w
                               "v" h
                               "z")}]
               [:path {:d (str "M 0,0"
                               "h" width)}]
               [:path {:d (str "M 0," height
                               "h" width)}]
               [:path {:d (str "M" width "," middle-y
                               "h" (- w))}]
               [:path {:d (str "M" width "," middle-y
                               "v" (- h))}]
               [:path {:d (str "M" width "," height
                               "v" (- h))}]]}))

(defn- potent-en-point [part-width part-height]
  (let [width part-width
        height (* 2 part-height)
        middle-x (/ width 2)
        middle-y (/ height 2)
        w (/ width 4)
        h (/ middle-y 2)]
    {:width width
     :height height
     :pattern [:<>
               [:path {:d (str "M 0," middle-y
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "H" width
                               "v" (* 2 h)
                               "h" (- w)
                               "v" (- h)
                               "h" (- w)
                               "v" h
                               "h" (- w)
                               "v" h
                               "H 0"
                               "z")}]]
     :outline [:<>
               [:path {:d (str "M 0," middle-y
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" (- h)
                               "h" w
                               "v" h
                               "H" width
                               "v" h)}]
               [:path {:d (str "M " width "," height
                               "v" (- h)
                               "h" (- w)
                               "v" (- h)
                               "h" (- w)
                               "v" h
                               "h" (- w)
                               "v" h
                               "H 0"
                               "v" (- h))}]
               [:path {:d (str "M 0,0"
                               "h" w)}]
               [:path {:d (str "M" width ",0"
                               "h" w)}]
               [:path {:d (str "M 0," middle-y
                               "v" (- h))}]
               [:path {:d (str "M" middle-x "," height
                               "h" w)}]]}))

(defn- render [context {:keys [start part-width part-height
                               rotation]}]
  (let [variant (interface/get-sanitized-data (c/++ context :variant))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        pattern-id (uid/generate "potenty")
        potent-function (case variant
                          :counter potent-counter
                          :in-pale potent-in-pale
                          :en-point potent-en-point
                          potent-default)
        {pattern-width :width
         pattern-height :height
         potent-pattern :pattern
         potent-outline :outline} (potent-function part-width part-height)]
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
          potent-outline]])
      (into [:<>]
            (map (fn [idx]
                   ^{:key idx}
                   [:pattern {:id (str pattern-id "-" idx)
                              :width pattern-width
                              :height pattern-height
                              :x (:x start)
                              :y (:y start)
                              :pattern-units "userSpaceOnUse"}
                    [:rect {:x 0
                            :y 0
                            :width pattern-width
                            :height pattern-height
                            :fill (get ["#000000" "#ffffff"] idx)}]
                    [:g {:fill (get ["#ffffff" "#000000"] idx)}
                     potent-pattern]]))
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
                      (c/++ context :fields idx)
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
                       (/ (units num-fields-x))
                       (* 4)
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
     :render-fn render}))

(defmethod interface/subfield-environments field-type [_context _properties]
  nil)

(defmethod interface/subfield-render-shapes field-type [_context _properties]
  nil)
