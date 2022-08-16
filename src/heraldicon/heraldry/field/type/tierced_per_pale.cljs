(ns heraldicon.heraldry.field.type.tierced-per-pale
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

(def field-type :heraldry.field.type/tierced-per-pale)

(defmethod field.interface/display-name field-type [_] :string.field.type/tierced-per-pale)

(defmethod field.interface/part-names field-type [_] ["dexter" "fess" "sinister"])

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))]
    {:anchor {:point {:type :option.type/choice
                      :choices (position/anchor-choices
                                [:fess
                                 :dexter
                                 :sinister
                                 :hoist
                                 :fly
                                 :left
                                 :center
                                 :right])
                      :default :fess
                      :ui/label :string.option/point}
              :offset-x {:type :option.type/range
                         :min -45
                         :max 45
                         :default 0
                         :ui/label :string.option/offset-x
                         :ui/step 0.1}
              :ui/label :string.option/anchor
              :ui/element :ui.element/position}
     :layout {:stretch-x {:type :option.type/range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch-x
                          :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style
     :opposite-line opposite-line-style}))

(defmethod interface/properties field-type [context]
  (let [{:keys [width]
         :as parent-environment} (interface/get-parent-environment context)
        {:keys [top bottom]} (:points parent-environment)
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        middle-width (-> width
                         (/ 3)
                         (* stretch-x))
        edge-1-x (- (:x anchor-point) (/ middle-width 2))
        edge-2-x (+ edge-1-x middle-width)
        parent-shape (interface/get-exact-parent-shape context)
        [edge-1-top edge-1-bottom] (v/intersections-with-shape
                                    (v/Vector. edge-1-x (:y top)) (v/Vector. edge-1-x (:y bottom))
                                    parent-shape :default? true)
        [edge-2-top edge-2-bottom] (v/intersections-with-shape
                                    (v/Vector. edge-2-x (:y top)) (v/Vector. edge-2-x (:y bottom))
                                    parent-shape :default? true)
        edge-2-top (assoc edge-2-top :y (:y edge-1-top))
        line-length (- (:y edge-1-bottom) (:y edge-1-top))]
    (post-process/properties
     {:type field-type
      :edge-1 [edge-1-top edge-1-bottom]
      :edge-2 [edge-2-top edge-2-bottom]
      :line-length line-length
      :percentage-base width
      :num-subfields 3}
     context)))

(defmethod interface/subfield-environments field-type [context {[edge-1-top edge-1-bottom] :edge-1
                                                                [edge-2-top edge-2-bottom] :edge-2}]
  (let [{:keys [meta points]} (interface/get-parent-environment context)
        {:keys [top-left top-right]} points]
    {:subfields [(environment/create
                  {:paths nil}
                  (-> meta
                      (dissoc :context)
                      (assoc :bounding-box (bb/from-points [top-left edge-1-top edge-1-bottom]))))
                 (environment/create
                  {:paths nil}
                  (-> meta
                      (dissoc :context)
                      (assoc :bounding-box (bb/from-points [edge-1-top edge-1-bottom
                                                            edge-2-top edge-2-bottom]))))
                 (environment/create
                  {:paths nil}
                  (-> meta
                      (dissoc :context)
                      (assoc :bounding-box (bb/from-points [top-right edge-2-top edge-2-bottom]))))]}))

(defmethod interface/subfield-render-shapes field-type [context {:keys [line opposite-line]
                                                                 [edge-1-top edge-1-bottom] :edge-1
                                                                 [edge-2-top edge-2-bottom] :edge-2}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        bounding-box (:bounding-box meta)
        {line-edge-1 :line
         line-edge-1-start :line-start
         line-edge-1-from :adjusted-from
         line-edge-1-to :adjusted-to
         :as line-edge-1-data} (line/create-with-extension line
                                                           edge-1-top edge-1-bottom
                                                           bounding-box
                                                           :context context)
        {line-edge-2 :line
         line-edge-2-start :line-start
         line-edge-2-from :adjusted-from
         line-edge-2-to :adjusted-to
         :as line-edge-2-data} (line/create-with-extension opposite-line
                                                           edge-2-top edge-2-bottom
                                                           bounding-box
                                                           :reversed? true
                                                           :flipped? true
                                                           :mirrored? true
                                                           :context context)]
    {:subfields [{:shape [(path/make-path
                           ["M" (v/add line-edge-1-from line-edge-1-start)
                            (path/stitch line-edge-1)
                            (infinity/clockwise
                             line-edge-1-to
                             (v/add line-edge-1-from line-edge-1-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-1-from line-edge-1-start)
                            (path/stitch line-edge-1)
                            (infinity/counter-clockwise
                             line-edge-1-to
                             (v/add line-edge-2-to line-edge-2-start))
                            (path/stitch line-edge-2)
                            (infinity/counter-clockwise
                             line-edge-2-from
                             (v/add line-edge-1-from line-edge-1-start))
                            "z"])]}
                 {:shape [(path/make-path
                           ["M" (v/add line-edge-2-to line-edge-2-start)
                            (path/stitch line-edge-2)
                            (infinity/clockwise
                             line-edge-2-from
                             (v/add line-edge-2-to line-edge-2-start))
                            "z"])]}]
     :lines [{:line line
              :line-from line-edge-1-from
              :line-data [line-edge-1-data]}
             {:line opposite-line
              :line-from line-edge-2-to
              :line-data [line-edge-2-data]}]}))
