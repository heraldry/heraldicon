(ns heraldicon.heraldry.ordinary.type.label
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/label)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/label)

(def ^:private variant-choices
  [[:string.option.variant-label-choice/full :full]
   [:string.option.variant-label-choice/truncated :truncated]])

(def variant-map
  (options/choices->map variant-choices))

(defmethod ordinary.interface/options ordinary-type [context]
  (let [num-points (or (interface/get-raw-data (c/++ context :num-points))
                       3)]
    (-> {:anchor {:point {:type :option.type/choice
                          :choices (position/anchor-choices
                                    [:fess
                                     :chief
                                     :base
                                     :honour
                                     :nombril
                                     :top
                                     :center
                                     :bottom])
                          :default :chief
                          :ui/label :string.option/point}
                  :alignment {:type :option.type/choice
                              :choices position/alignment-choices
                              :default :middle
                              :ui/label :string.option/alignment
                              :ui/element :ui.element/radio-select}
                  :offset-y {:type :option.type/range
                             :min -45
                             :max 45
                             :default 0
                             :ui/label :string.option/offset-y
                             :ui/step 0.1}
                  :ui/label :string.option/anchor
                  :ui/element :ui.element/position}
         :variant {:type :option.type/choice
                   :choices variant-choices
                   :default :full
                   :ui/label :string.option/variant
                   :ui/element :ui.element/radio-select}
         :num-points {:type :option.type/range
                      :min 1
                      :max 16
                      :default 3
                      :integer? true
                      :ui/label :string.option/number-of-points}
         :geometry {:size {:type :option.type/range
                           :min 2
                           :max 90
                           :default (case num-points
                                      1 10
                                      2 10
                                      3 10
                                      4 9
                                      5 8
                                      6 7
                                      7 6
                                      5)
                           :ui/label :string.option/size
                           :ui/step 0.1}
                    :width {:type :option.type/range
                            :min 10
                            :max 150
                            :default (case num-points
                                       1 66
                                       2 66
                                       3 66
                                       4 76
                                       5 80
                                       6 84
                                       7 87
                                       90)
                            :ui/label :string.option/width
                            :ui/step 0.1}
                    :thickness {:type :option.type/range
                                :min 0
                                :max 20
                                :default 5
                                :ui/label :string.option/bar-thickness
                                :ui/step 0.1}
                    :eccentricity {:type :option.type/range
                                   :min 0
                                   :max 1
                                   :default 0
                                   :ui/label :string.option/eccentricity
                                   :ui/step 0.01}
                    :stretch {:type :option.type/range
                              :min 0.33
                              :max 10
                              :default (case num-points
                                         1 2
                                         2 2
                                         3 2
                                         4 2.25
                                         5 2.5
                                         6 2.75
                                         7 3
                                         3.25)
                              :ui/label :string.option/stretch
                              :ui/step 0.01}
                    :ui/label :string.option/geometry
                    :ui/element :ui.element/geometry}
         :outline? options/plain-outline?-option
         :fimbriation (-> (fimbriation/options (c/++ context :fimbriation))
                          (options/override-if-exists [:alignment :default] :outside))}
        (ordinary.shared/add-humetty-and-voided context)
        (options/override-if-exists [:voided :thickness :default] 25))))

(defn- start-and-end [truncated? y label-start label-width parent-shape left-x right-x]
  (if truncated?
    [(v/Vector. label-start y) (v/Vector. (+ label-start label-width) y)]
    (v/intersections-with-shape
     (v/Vector. left-x y) (v/Vector. right-x y)
     parent-shape :default? true)))

(defmethod interface/properties ordinary-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        points (:points parent-environment)
        {:keys [left right]} points
        variant (interface/get-sanitized-data (c/++ context :variant))
        truncated? (= variant :truncated)
        num-points (interface/get-sanitized-data (c/++ context :num-points))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        thickness (interface/get-sanitized-data (c/++ context :geometry :thickness))
        eccentricity (interface/get-sanitized-data (c/++ context :geometry :eccentricity))
        stretch (interface/get-sanitized-data (c/++ context :geometry :stretch))
        percentage-base-height (:height parent-environment)
        band-size (math/percent-of percentage-base-height thickness)
        percentage-base-width (:width parent-environment)
        label-width (math/percent-of percentage-base-width
                                     (interface/get-sanitized-data (c/++ context :geometry :width)))
        point-width (math/percent-of percentage-base-width size)
        point-height (* point-width stretch)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :chief)
        upper (case (:alignment anchor)
                :left (:y anchor-point)
                :right (- (:y anchor-point) band-size)
                (- (:y anchor-point) (/ band-size 2)))
        label-start (-> anchor-point
                        :x
                        (- (/ label-width 2)))
        point-extra (-> point-width
                        (/ 2)
                        (* eccentricity))
        parent-shape (interface/get-exact-parent-shape context)
        [upper-left upper-right] (start-and-end truncated? upper label-start label-width
                                                parent-shape (:x left) (:x right))
        projected-extra (-> point-extra
                            (/ point-height)
                            (* 2)
                            (* band-size))
        [upper-left upper-right] (if truncated?
                                   [(v/add upper-left (v/Vector. (+ point-extra projected-extra) 0))
                                    (v/sub upper-right (v/Vector. (+ point-extra projected-extra) 0))]
                                   [upper-left upper-right])
        lower (+ upper band-size)
        [point-left point-right] [(v/Vector. label-start lower) (v/Vector. (+ label-start label-width) lower)]
        [point-left point-right] (if truncated?
                                   [(v/add point-left (v/Vector. point-extra 0))
                                    (v/sub point-right (v/Vector. point-extra 0))]
                                   [point-left point-right])
        [lower-left lower-right] (if truncated?
                                   [point-left point-right]
                                   (start-and-end truncated? lower label-start label-width
                                                  parent-shape (:x left) (:x right)))
        point-distance (/ (- label-width point-width) (max (dec num-points) 1))
        initial-shift (if (< num-points 2)
                        (/ label-width 2)
                        (/ point-width 2))
        point-centers (mapv (fn [idx]
                              (+ label-start
                                 initial-shift
                                 (* idx point-distance)))
                            (range num-points))]
    (post-process/properties
     {:type ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :point-edge [point-left point-right]
      :truncated? truncated?
      :num-points num-points
      :label-width label-width
      :label-start label-start
      :eccentricity eccentricity
      :point-width point-width
      :point-height point-height
      :point-centers point-centers
      :point-extra point-extra
      :band-size band-size
      :percentage-base-height percentage-base-height
      :percentage-base-width percentage-base-width
      :humetty-percentage-base (:width parent-environment)
      :voided-percentage-base (min band-size point-width)}
     context)))

(defmethod interface/environment ordinary-type [_context {:keys [point-height]
                                                          [upper-left upper-right] :upper
                                                          [lower-left lower-right] :lower}]
  (let [bounding-box-points [upper-left upper-right
                             (v/add lower-left (v/Vector. 0 point-height))
                             (v/add lower-right (v/Vector. 0 point-height))]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context {:keys [truncated? point-width point-height
                                                                 point-centers point-extra]
                                                          [upper-left upper-right] :upper
                                                          [lower-left lower-right] :lower
                                                          [point-left point-right] :point-edge
                                                          :as properties}]
  (let [point-y (:y point-left)
        point-width-half (/ point-width 2)
        [upper-left lower-left] (if-not truncated?
                                  (map #(v/sub % (v/Vector. 30 0)) [upper-left lower-left])
                                  [upper-left lower-left])
        [upper-right lower-right] (if-not truncated?
                                    (map #(v/add % (v/Vector. 30 0)) [upper-right lower-right])
                                    [upper-right lower-right])
        shape-points (concat [point-left
                              lower-left
                              upper-left
                              upper-right
                              lower-right
                              point-right]
                             (mapcat (fn [point-x]
                                       [(v/Vector. (-> point-x (+ point-width-half) (- point-extra)) point-y)
                                        (v/Vector. (-> point-x (+ point-width-half) (+ point-extra)) (+ point-y point-height))
                                        (v/Vector. (-> point-x (- point-width-half) (- point-extra)) (+ point-y point-height))
                                        (v/Vector. (-> point-x (- point-width-half) (+ point-extra)) point-y)])
                                     (reverse point-centers))
                             [point-left])
        shape (path/make-path
               (-> (into ["M" point-left]
                         (mapcat (fn [p]
                                   ["L" p])
                                 shape-points))
                   (conj "z")))]
    (post-process/shape
     {:shape [shape]
      :lines [{:edge-paths [shape]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [_context _properties]
  nil)
