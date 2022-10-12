(ns heraldicon.heraldry.field.type.quarterly
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
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
                              :max 16
                              :default 2
                              :integer? true
                              :ui/label :string.option/base-fields
                              :ui/element :ui.element/field-layout-num-base-fields}
            :base-field-shift {:type :option.type/range
                               :min -16
                               :max 16
                               :default 1
                               :integer? true
                               :ui/label :string.option/base-field-shift
                               :ui/element :ui.element/field-layout-base-field-shift}
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

(defmethod interface/properties field-type [context]
  (let [{:keys [width height points]} (interface/get-subfields-environment context)
        {:keys [center]} points
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        required-width (* part-width
                          num-fields-x)
        part-height (-> height
                        (/ num-fields-y)
                        (* stretch-y))
        required-height (* part-height
                           num-fields-y)
        x0 (-> (:x center)
               (- (/ required-width 2))
               (+ (* offset-x
                     part-width)))
        y0 (-> (:y center)
               (- (/ required-height 2))
               (+ (* offset-y
                     part-height)))
        start (v/Vector. x0 y0)
        part-size (v/Vector. part-width part-height)
        parts (vec (for [j (range num-fields-y)
                         i (range num-fields-x)]
                     (let [part-top-left (v/add start (v/dot (v/Vector. i j) part-size))
                           part-bottom-right (v/add part-top-left part-size)]
                       [part-top-left part-bottom-right])))]
    {:type field-type
     :num-fields-x num-fields-x
     :num-fields-y num-fields-y
     :x-values (vec (map (fn [i]
                           (+ (:x start) (* i (:x part-size))))
                         (range 1 num-fields-x)))
     :y-values (vec (map (fn [j]
                           (+ (:y start) (* j (:y part-size))))
                         (range 1 num-fields-y)))
     :parts parts
     :part-size part-size
     :num-subfields (* num-fields-x num-fields-y)}))

(defmethod interface/subfield-environments field-type [_context {:keys [parts]}]
  {:subfields (mapv (fn [[part-top-left part-bottom-right]]
                      (environment/create (bb/from-points [part-top-left part-bottom-right])))
                    parts)})

(defmethod interface/subfield-render-shapes field-type [context {:keys [parts num-fields-x num-fields-y
                                                                        x-values y-values]}]
  (let [{:keys [points]} (interface/get-subfields-environment context)
        {:keys [top bottom left right]} points
        min-x (- (:x left) 50)
        max-x (+ (:x right) 50)
        min-y (- (:y top) 50)
        max-y (+ (:y bottom) 50)]
    {:subfields (into []
                      (map-indexed (fn [index [part-top-left part-bottom-right]]
                                     (let [i (mod index num-fields-x)
                                           j (quot index num-fields-x)
                                           first-x? (zero? i)
                                           first-y? (zero? j)
                                           last-x? (= i (dec num-fields-x))
                                           last-y? (= j (dec num-fields-y))
                                           part-top-left (cond-> part-top-left
                                                           first-x? (assoc :x min-x)
                                                           first-y? (assoc :y min-y))
                                           part-bottom-right (cond-> part-bottom-right
                                                               last-x? (assoc :x max-x)
                                                               last-y? (assoc :y max-y))]
                                       {:shape [(path/make-path
                                                 ["M" part-top-left
                                                  "H" (:x part-bottom-right)
                                                  "V" (:y part-bottom-right)
                                                  "H" (:x part-top-left)
                                                  "z"])]})))
                      parts)
     :edges (-> []
                (into (map (fn [x]
                             {:paths [(path/make-path
                                       ["M" (v/Vector. x min-y)
                                        "V" max-y])]}))
                      x-values)
                (into (map (fn [y]
                             {:paths [(path/make-path
                                       ["M" (v/Vector. min-x y)
                                        "H" max-x])]}))
                      y-values))}))
