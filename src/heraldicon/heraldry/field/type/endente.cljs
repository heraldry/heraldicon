(ns heraldicon.heraldry.field.type.endente
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.path :as path]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/endente)

(defmethod field.interface/display-name field-type [_] :string.field.type/endente)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [_context]
  {:layout {:num-fields-x {:type :option.type/range
                           :min 1
                           :max 20
                           :default 6
                           :ui/label :string.option/subfields-x
                           :ui/element :ui.element/field-layout-num-fields-x}
            :num-fields-y {:type :option.type/range
                           :min 1
                           :max 20
                           :default 10
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
                        :min 0.2
                        :max 3
                        :default 1
                        :ui/label :string.option/stretch-x
                        :ui/step 0.01}
            :stretch-y {:type :option.type/range
                        :min 0.2
                        :max 3
                        :default 1
                        :ui/label :string.option/stretch-y
                        :ui/step 0.01}
            :ui/label :string.option/layout
            :ui/element :ui.element/field-layout}})

(defn- render [context {:keys [start part-width part-height]}]
  (let [outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        pattern-id (uid/generate "lozengy")
        mx (/ part-width 2)
        my (/ part-height 2)]
    [:<>
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
                   :width part-width
                   :height part-height
                   :x (:x start)
                   :y (:y start)
                   :pattern-units "userSpaceOnUse"}
         [:g (outline/style context)
          [:path {:d (path/make-path ["M" [0 0]
                                      "L" [part-width part-height]])}]
          [:path {:d (path/make-path ["M" [part-width 0]
                                      "L" [0 part-height]
                                      "z"])}]
          [:path {:d (path/make-path ["M" [0 my]
                                      "L" [part-width my]
                                      "z"])}]
          [:path {:d (path/make-path ["M" [0 0]
                                      "L" [part-width 0]
                                      "z"])}]
          [:path {:d (path/make-path ["M" [0 part-height]
                                      "L" [part-width part-height]
                                      "z"])}]]])
      [:pattern {:id (str pattern-id "-0")
                 :width part-width
                 :height part-height
                 :x (:x start)
                 :y (:y start)
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x 0
               :y 0
               :width part-width
               :height part-height
               :shape-rendering "crispEdges"
               :fill "#ffffff"}]
       [:path {:d (path/make-path ["M" [0 0]
                                   "L" [mx my]
                                   "L" [part-width 0]
                                   "z"])
               :fill "#000000"}]
       [:path {:d (path/make-path ["M" [mx my]
                                   "L" [0 part-height]
                                   "L" [0 my]
                                   "z"])
               :fill "#000000"}]
       [:path {:d (path/make-path ["M" [mx my]
                                   "L" [part-width my]
                                   "L" [part-width part-height]
                                   "z"])
               :fill "#000000"}]]
      [:pattern {:id (str pattern-id "-1")
                 :width part-width
                 :height part-height
                 :x (:x start)
                 :y (:y start)
                 :pattern-units "userSpaceOnUse"}
       [:rect {:x 0
               :y 0
               :width part-width
               :height part-height
               :shape-rendering "crispEdges"
               :fill "#000000"}]
       [:path {:d (path/make-path ["M" [0 0]
                                   "L" [mx my]
                                   "L" [part-width 0]
                                   "z"])
               :fill "#ffffff"}]
       [:path {:d (path/make-path ["M" [mx my]
                                   "L" [0 part-height]
                                   "L" [0 my]
                                   "z"])
               :fill "#ffffff"}]
       [:path {:d (path/make-path ["M" [mx my]
                                   "L" [part-width my]
                                   "L" [part-width part-height]
                                   "z"])
               :fill "#ffffff"}]]]
     [:g
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
                :fill (str "url(#" pattern-id "-outline)")}])]]))

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
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        ; sqrt(3)
        aspect-ratio 1.7320508075688772
        unstretched-part-height (if raw-num-fields-y
                                  (/ height 0.5 num-fields-y)
                                  (* part-width aspect-ratio))
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
     :render-fn render}))

(defmethod interface/subfield-environments field-type [_context]
  nil)

(defmethod interface/subfield-render-shapes field-type [_context]
  nil)
