(ns heraldicon.heraldry.field.type.chequy
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/chequy)

(defmethod field.interface/display-name field-type [_] :string.field.type/chequy)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [_context]
  {:layout {:num-fields-x {:type :option.type/range
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
            :num-base-fields {:type :option.type/range
                              :min 2
                              :max 8
                              :default 2
                              :integer? true
                              :ui/label :string.option/base-fields
                              :ui/element :ui.element/field-layout-num-base-fields}
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
            :ui/label :string.option/layout
            :ui/element :ui.element/field-layout}})

(defn- render [context {:keys [start part-width part-height]}]
  (let [num-base-fields (interface/get-sanitized-data (c/++ context :layout :num-base-fields))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        pattern-id (uid/generate "chequy")]
    [:g
     [:defs
      (when outline?
        [:pattern {:id (str pattern-id "-outline")
                   :width part-width
                   :height part-height
                   :x (:x start)
                   :y (:y start)
                   :pattern-units "userSpaceOnUse"}
         [:g (outline/style context)
          [:path {:d (str "M 0,0 h " part-width)}]
          [:path {:d (str "M 0,0 v " part-height)}]
          [:path {:d (str "M 0," part-height " h " part-width)}]
          [:path {:d (str "M " part-width ",0 v " part-height)}]]])
      (into [:<>]
            (map (fn [idx]
                   ^{:key idx}
                   [:pattern {:id (str pattern-id "-" idx)
                              :width (* part-width num-base-fields)
                              :height (* part-height num-base-fields)
                              :x (:x start)
                              :y (:y start)
                              :pattern-units "userSpaceOnUse"}
                    [:rect {:x 0
                            :y 0
                            :width (* part-width num-base-fields)
                            :height (* part-height num-base-fields)
                            :fill "#000000"}]
                    (into [:<>]
                          (for [j (range num-base-fields)
                                i (range num-base-fields)]
                            (when (-> i (+ j) (mod num-base-fields) (= idx))
                              ^{:key [i j]}
                              [:rect {:x (* i part-width)
                                      :y (* j part-height)
                                      :width part-width
                                      :height part-height
                                      :fill "#ffffff"}])))]))
            (range num-base-fields))]
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
           (range num-base-fields))
     (when outline?
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill (str "url(#" pattern-id "-outline)")}])]))

(defmethod interface/properties field-type [context]
  (let [{:keys [width height points]} (interface/get-effective-environment context)
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
     :render-fn render}))

(defmethod interface/subfield-environments field-type [_context _properties]
  nil)

(defmethod interface/subfield-render-shapes field-type [_context _properties]
  nil)
