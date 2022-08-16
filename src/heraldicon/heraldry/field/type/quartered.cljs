(ns heraldicon.heraldry.field.type.quartered
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
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def field-type :heraldry.field.type/quartered)

(defmethod field.interface/display-name field-type [_] :string.field.type/quartered)

(defmethod field.interface/part-names field-type [_] ["I" "II" "III" "IV"])

(defmethod field.interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))]
    {:anchor {:point {:type :option.type/choice
                      :choices (position/anchor-choices
                                [:chief
                                 :base
                                 :fess
                                 :dexter
                                 :sinister
                                 :honour
                                 :nombril
                                 :hoist
                                 :fly
                                 :center])
                      :default :fess
                      :ui/label :string.option/point}
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
     :line line-style
     :opposite-line opposite-line-style
     :outline? options/plain-outline?-option}))

(defmethod interface/properties field-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        {:keys [top bottom left right]} (:points parent-environment)
        percentage-base (:height parent-environment)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        {edge-x :x
         edge-y :y} anchor-point
        parent-shape (interface/get-exact-parent-shape context)
        [left-end right-end] (v/intersections-with-shape
                              (v/Vector. (:x left) edge-y) (v/Vector. (:x right) edge-y)
                              parent-shape :default? true)
        [top-end bottom-end] (v/intersections-with-shape
                              (v/Vector. edge-x (:y top)) (v/Vector. edge-x (:y bottom))
                              parent-shape :default? true)
        line-length (->> [top-end bottom-end
                          left-end right-end]
                         (map (fn [v]
                                (v/sub v anchor-point)))
                         (map v/abs)
                         (apply max))]
    (post-process/properties
     {:type field-type
      :edge-left [anchor-point left-end]
      :edge-right [anchor-point right-end]
      :edge-top [anchor-point top-end]
      :edge-bottom [anchor-point bottom-end]
      :line-length line-length
      :percentage-base percentage-base
      :num-subfields 4}
     context)))

(defmethod interface/subfield-environments field-type [context {[edge-top-1 edge-top-2] :edge-top
                                                                [_edge-bottom-1 edge-bottom-2] :edge-bottom
                                                                [_edge-left-1 edge-left-2] :edge-left
                                                                [_edge-right-1 edge-right-2] :edge-right}]
  (let [{:keys [meta points]} (interface/get-parent-environment context)
        {:keys [top-left top-right
                bottom-left bottom-right]} points]
    {:subfields [(environment/create
                  {:paths nil}
                  (-> meta
                      (dissoc :context)
                      (assoc :bounding-box (bb/from-points [top-left edge-top-1
                                                            edge-left-2 edge-top-2]))))
                 (environment/create
                  {:paths nil}
                  (-> meta
                      (dissoc :context)
                      (assoc :bounding-box (bb/from-points [top-right edge-top-1
                                                            edge-top-2 edge-right-2]))))
                 (environment/create
                  {:paths nil}
                  (-> meta
                      (dissoc :context)
                      (assoc :bounding-box (bb/from-points [bottom-left edge-top-1
                                                            edge-left-2 edge-bottom-2]))))
                 (environment/create
                  {:paths nil}
                  (-> meta
                      (dissoc :context)
                      (assoc :bounding-box (bb/from-points [bottom-right edge-top-1
                                                            edge-right-2 edge-bottom-2]))))]}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line opposite-line]
                                                                 [edge-top-1 edge-top-2] :edge-top
                                                                 [edge-bottom-1 edge-bottom-2] :edge-bottom
                                                                 [edge-left-1 edge-left-2] :edge-left
                                                                 [edge-right-1 edge-right-2] :edge-right}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        bounding-box (:bounding-box meta)
        {line-edge-top :line
         line-edge-top-start :line-start
         line-edge-top-to :adjusted-to
         :as line-edge-top-data} (line/create-with-extension line
                                                             edge-top-1 edge-top-2
                                                             bounding-box
                                                             :reversed? true
                                                             :extend-from? false
                                                             :context context)
        {line-edge-bottom :line
         line-edge-bottom-start :line-start
         line-edge-bottom-to :adjusted-to
         :as line-edge-bottom-data} (line/create-with-extension line
                                                                edge-bottom-1 edge-bottom-2
                                                                bounding-box
                                                                :reversed? true
                                                                :extend-from? false
                                                                :context context)
        {line-edge-left :line
         line-edge-left-from :adjusted-from
         line-edge-left-to :adjusted-to
         :as line-edge-left-data} (line/create-with-extension opposite-line
                                                              edge-left-1 edge-left-2
                                                              bounding-box
                                                              :mirrored? true
                                                              :flipped? true
                                                              :extend-from? false
                                                              :context context)
        {line-edge-right :line
         line-edge-right-from :adjusted-from
         line-edge-right-to :adjusted-to
         :as line-edge-right-data} (line/create-with-extension opposite-line
                                                               edge-right-1 edge-right-2
                                                               bounding-box
                                                               :mirrored? true
                                                               :flipped? true
                                                               :extend-from? false
                                                               :context context)]
    {:subfields [{:shape [(path/make-path
                           ["M" (v/add line-edge-top-to line-edge-top-start)
                            (path/stitch line-edge-top)
                            (path/stitch line-edge-left)
                            (infinity/clockwise
                             line-edge-left-to
                             (v/add line-edge-top-to line-edge-top-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-top-to line-edge-top-start)
                            (path/stitch line-edge-top)
                            (path/stitch line-edge-right)
                            (infinity/counter-clockwise
                             line-edge-right-to
                             (v/add line-edge-top-to line-edge-top-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-bottom-to line-edge-bottom-start)
                            (path/stitch line-edge-bottom)
                            (path/stitch line-edge-left)
                            (infinity/counter-clockwise
                             line-edge-left-to
                             (v/add line-edge-bottom-to line-edge-bottom-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-bottom-to line-edge-bottom-start)
                            (path/stitch line-edge-bottom)
                            (path/stitch line-edge-right)
                            (infinity/clockwise
                             line-edge-right-to
                             (v/add line-edge-bottom-to line-edge-bottom-start))
                            "z"])]}]
     :lines [{:line line
              :line-from line-edge-top-to
              :line-data [line-edge-top-data]}
             {:line line
              :line-from line-edge-bottom-to
              :line-data [line-edge-bottom-data]}
             {:line opposite-line
              :line-from line-edge-left-from
              :line-data [line-edge-left-data]}
             {:line opposite-line
              :line-from line-edge-right-from
              :line-data [line-edge-right-data]}]}))
