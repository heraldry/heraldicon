(ns heraldicon.heraldry.field.type.paly
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/paly)

(defmethod field.interface/display-name field-type [_] :string.field.type/paly)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)]
    {:layout {:num-fields-x {:type :option.type/range
                             :min 1
                             :max 20
                             :default 6
                             :integer? true
                             :ui/label :string.option/subfields-x
                             :ui/element :ui.element/field-layout-num-fields-x}
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
              :stretch-x {:type :option.type/range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch-x
                          :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (- (:x bottom-right)
                 (:x top-left))
        pallet-width (-> width
                         (/ num-fields-x)
                         (* stretch-x))
        required-width (* pallet-width
                          num-fields-x)
        middle (-> width
                   (/ 2)
                   (+ (:x top-left)))
        x0 (-> middle
               (- (/ required-width 2))
               (+ (* offset-x
                     pallet-width)))
        y1 (:y top-left)
        y2 (:y bottom-right)
        height (- y2 y1)
        {line-down :line
         line-down-start :line-start
         line-down-end :line-end} (line/create line
                                               top-left
                                               (v/add top-left (v/Vector. 0 height))
                                               :real-start 0
                                               :real-end height
                                               :context context
                                               :environment environment)
        {line-up :line
         line-up-start :line-start
         line-up-end :line-end} (line/create line
                                             top-left
                                             (v/add top-left (v/Vector. 0 height))
                                             :flipped? true
                                             :mirrored? true
                                             :reversed? true
                                             :real-start 0
                                             :real-end height
                                             :context context
                                             :environment environment)
        parts (->> (range num-fields-x)
                   (map (fn [i]
                          (let [x1 (+ x0 (* i pallet-width))
                                x2 (+ x1 pallet-width)
                                last-part? (-> i inc (= num-fields-x))
                                line-one-top (v/Vector. x1 y1)
                                line-one-bottom (v/Vector. x1 y2)
                                line-two-top (v/Vector. x2 y1)
                                line-two-bottom (v/Vector. x2 y2)]
                            [(cond
                               (and (zero? i)
                                    last-part?) ["M" -1000 -1000
                                                 "h" 2000
                                                 "v" 2000
                                                 "h" -2000
                                                 "z"]
                               (zero? i) ["M" (v/add line-two-top
                                                     line-down-start)
                                          (path/stitch line-down)
                                          (infinity/path :clockwise
                                                         [:bottom :top]
                                                         [(v/add line-two-bottom
                                                                 line-down-end)
                                                          (v/add line-two-top
                                                                 line-down-start)])
                                          "z"]
                               (even? i) (concat
                                          ["M" (v/add line-one-bottom
                                                      line-up-start)
                                           (path/stitch line-up)]
                                          (cond
                                            last-part? [(infinity/path :clockwise
                                                                       [:top :bottom]
                                                                       [(v/add line-one-top
                                                                               line-up-end)
                                                                        (v/add line-one-bottom
                                                                               line-up-start)])
                                                        "z"]
                                            :else [(infinity/path :clockwise
                                                                  [:top :top]
                                                                  [(v/add line-one-top
                                                                          line-up-end)
                                                                   (v/add line-two-top
                                                                          line-down-start)])
                                                   (path/stitch line-down)
                                                   (infinity/path :clockwise
                                                                  [:bottom :bottom]
                                                                  [(v/add line-one-bottom
                                                                          line-down-end)
                                                                   (v/add line-two-bottom
                                                                          line-up-start)])
                                                   "z"]))
                               :else (concat
                                      ["M" (v/add line-one-top
                                                  line-down-start)
                                       (path/stitch line-down)]
                                      (cond
                                        last-part? [(infinity/path :counter-clockwise
                                                                   [:bottom :top]
                                                                   [(v/add line-one-bottom
                                                                           line-down-end)
                                                                    (v/add line-one-top
                                                                           line-down-start)])
                                                    "z"]
                                        :else [(infinity/path :counter-clockwise
                                                              [:bottom :bottom]
                                                              [(v/add line-one-bottom
                                                                      line-down-end)
                                                               (v/add line-two-bottom
                                                                      line-up-start)])
                                               (path/stitch line-up)
                                               (infinity/path :clockwise
                                                              [:top :top]
                                                              [(v/add line-two-top
                                                                      line-up-end)
                                                               (v/add line-one-top
                                                                      line-down-start)])
                                               "z"])))
                             [line-one-top line-two-bottom]])))
                   vec)
        edges (->> num-fields-x
                   dec
                   range
                   (map (fn [i]
                          (let [x1 (+ x0 (* i pallet-width))
                                x2 (+ x1 pallet-width)
                                line-two-top (v/Vector. x2 y1)
                                line-two-bottom (v/Vector. x2 y2)]
                            (if (even? i)
                              (path/make-path ["M" (v/add line-two-top
                                                          line-down-start)
                                               (path/stitch line-down)])
                              (path/make-path ["M" (v/add line-two-bottom
                                                          line-up-start)
                                               (path/stitch line-up)])))))
                   vec)
        overlap (-> edges
                    (->> (map vector))
                    vec
                    (conj nil))
        outlines (when outline?
                   (into [:g (outline/style context)]
                         (map (fn [i]
                                ^{:key i}
                                [:path {:d (nth edges i)}]))
                         (range (dec num-fields-x))))]
    [:<>
     [shared/make-subfields
      context parts
      overlap
      environment]
     outlines]))
