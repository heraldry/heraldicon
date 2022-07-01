(ns heraldicon.heraldry.field.type.quarterly
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/quarterly)

(defmethod field.interface/display-name field-type [_] :string.field.type/quarterly)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [_context]
  {:layout {:num-fields-x {:type :option.type/range
                           :min 1
                           :max 20
                           :default 3
                           :integer? true
                           :ui/label :string.option/subfields-x
                           :ui/element :ui.element/field-layout-num-fields-x}
            :num-fields-y {:type :option.type/range
                           :min 1
                           :max 20
                           :default 4
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
            :rotation {:type :option.type/range
                       :min -90
                       :max 90
                       :default 0
                       :ui/label :string.option/rotation
                       :ui/step 0.01}
            :ui/label :string.option/layout
            :ui/element :ui.element/field-layout}})

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (- (:x bottom-right)
                 (:x top-left))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        required-width (* part-width
                          num-fields-x)
        middle-x (-> width
                     (/ 2)
                     (+ (:x top-left)))
        x0 (-> middle-x
               (- (/ required-width 2))
               (+ (* offset-x
                     part-width)))
        height (- (:y bottom-right)
                  (:y top-left))
        part-height (-> height
                        (/ num-fields-y)
                        (* stretch-y))
        required-height (* part-height
                           num-fields-y)
        middle-y (-> height
                     (/ 2)
                     (+ (:y top-left)))
        y0 (-> middle-y
               (- (/ required-height 2))
               (+ (* offset-y
                     part-height)))
        parts (vec (for [j (range num-fields-y)
                         i (range num-fields-x)]
                     (let [x1 (+ x0 (* i part-width))
                           x2 (+ x1 part-width)
                           y1 (+ y0 (* j part-height))
                           y2 (+ y1 part-height)
                           first-x? (zero? i)
                           first-y? (zero? j)
                           last-x? (-> num-fields-x dec (= i))
                           last-y? (-> num-fields-y dec (= j))]
                       (cond
                         (and first-x?
                              last-x?
                              first-y?
                              last-y?) [["M" -1000 -1000
                                         "h" 2000
                                         "v" 2000
                                         "h" -2000
                                         "z"]
                                        [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         (and first-x?
                              first-y?) [["M" [x1 y2]
                                          "L" [x2 y2]
                                          "L" [x2 y1]
                                          (infinity/path :counter-clockwise
                                                         [:top :left]
                                                         [(v/Vector. x2 y1) (v/Vector. x1 y2)])
                                          "z"]
                                         [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         (and last-x?
                              first-y?) [["M" [x1 y1]
                                          "L" [x1 y2]
                                          "L" [x2 y2]
                                          (infinity/path :counter-clockwise
                                                         [:right :top]
                                                         [(v/Vector. x2 y2) (v/Vector. x1 y1)])
                                          "z"]
                                         [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         (and first-x?
                              last-y?) [["M" [x1 y1]
                                         "L" [x2 y1]
                                         "L" [x2 y2]
                                         (infinity/path :clockwise
                                                        [:bottom :left]
                                                        [(v/Vector. x2 y2) (v/Vector. x1 y1)])
                                         "z"]
                                        [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         (and last-x?
                              last-y?) [["M" [x1 y2]
                                         "L" [x1 y1]
                                         "L" [x2 y1]
                                         (infinity/path :clockwise
                                                        [:right :bottom]
                                                        [(v/Vector. x2 y1) (v/Vector. x1 y2)])
                                         "z"]
                                        [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         first-x? [["M" [x1 y1]
                                    "L" [x2 y1]
                                    "L" [x2 y2]
                                    "L" [x1 y2]
                                    (infinity/path :counter-clockwise
                                                   [:left :left]
                                                   [(v/Vector. x1 y2) (v/Vector. x1 y1)])
                                    "z"]
                                   [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         first-y? [["M" [x1 y1]
                                    "L" [x1 y2]
                                    "L" [x2 y2]
                                    "L" [x2 y1]
                                    (infinity/path :counter-clockwise
                                                   [:top :top]
                                                   [(v/Vector. x2 y1) (v/Vector. x1 y1)])
                                    "z"]
                                   [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         last-x? [["M" [x2 y2]
                                   "L" [x1 y2]
                                   "L" [x1 y1]
                                   "L" [x2 y1]
                                   (infinity/path :clockwise
                                                  [:right :right]
                                                  [(v/Vector. x2 y1) (v/Vector. x2 y2)])
                                   "z"]
                                  [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         last-y? [["M" [x1 y2]
                                   "L" [x1 y1]
                                   "L" [x2 y1]
                                   "L" [x2 y2]
                                   (infinity/path :clockwise
                                                  [:bottom :bottom]
                                                  [(v/Vector. x2 y2) (v/Vector. x1 y2)])
                                   "z"]
                                  [(v/Vector. x1 y1) (v/Vector. x2 y2)]]
                         :else [["M" [x1 y1]
                                 "L" [x2 y1]
                                 "L" [x2 y2]
                                 "L" [x1 y2]
                                 "z"]
                                [(v/Vector. x1 y1) (v/Vector. x2 y2)]]))))
        overlap (vec (for [j (range num-fields-y)
                           i (range num-fields-x)]
                       (let [x1 (+ x0 (* i part-width))
                             x2 (+ x1 part-width)
                             y1 (+ y0 (* j part-height))
                             y2 (+ y1 part-height)]
                         [(path/make-path ["M" [x2 y1]
                                           "L" [x2 y2]
                                           "L" [x1 y2]])])))
        outline-extra 50
        outlines (when outline?
                   [:g (outline/style context)
                    (into [:<>]
                          (map (fn [i]
                                 (let [x1 (+ x0 (* i part-width))]
                                   ^{:key [:x i]}
                                   [:path {:d (path/make-path ["M" [x1 (- y0 outline-extra)]
                                                               "L" [x1 (+ y0 required-height outline-extra)]])}])))
                          (range 1 num-fields-x))
                    (into [:<>]
                          (map (fn [j]
                                 (let [y1 (+ y0 (* j part-height))]
                                   ^{:key [:y j]}
                                   [:path {:d (path/make-path ["M" [(- x0 outline-extra) y1]
                                                               "L" [(+ x0 required-width outline-extra) y1]])}])))
                          (range 1 num-fields-y))])]
    [:<>
     [shared/make-subfields
      context parts
      overlap
      environment]
     outlines]))
