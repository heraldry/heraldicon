(ns heraldicon.heraldry.ordinary.type.fess
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
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

(def ordinary-type :heraldry.ordinary.type/fess)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/fess)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:fess
                                  :chief
                                  :base
                                  :honour
                                  :nombril
                                  :top
                                  :center
                                  :bottom])
                       :default :fess
                       :ui/label :string.option/point}
               :alignment {:type :option.type/choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui/label :string.option/alignment
                           :ui/element :ui.element/radio-select}
               :offset-y {:type :option.type/range
                          :min -75
                          :max 75
                          :default 0
                          :ui/label :string.option/offset-y
                          :ui/step 0.1}
               :ui/label :string.option/anchor
               :ui/element :ui.element/position}
      :line line-style
      :opposite-line opposite-line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 90
                        :default 25
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        {:keys [left right]} (:points parent-environment)
        percentage-base (:height parent-environment)
        band-size (math/percent-of percentage-base size)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        upper (case (:alignment anchor)
                :left (:y anchor-point)
                :right (- (:y anchor-point) band-size)
                (- (:y anchor-point) (/ band-size 2)))
        lower (+ upper band-size)
        parent-shape (interface/get-exact-parent-shape context)
        [upper-left upper-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) upper) (v/Vector. (:x right) upper)
                                  parent-shape :default? true)
        [lower-left lower-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) lower) (v/Vector. (:x right) lower)
                                  parent-shape :default? true)
        line-length (- (:x upper-right) (:x upper-left))]
    (post-process/properties
     {:type ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (:width parent-environment)
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [context {[upper-left upper-right] :upper
                                                         [lower-left lower-right] :lower}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        bounding-box-points [upper-left upper-right
                             lower-left lower-right]]
    (environment/create
     {:paths nil}
     (-> meta
         (dissoc :context)
         (assoc :bounding-box (bb/from-points bounding-box-points))))))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line]
                                                          [upper-left upper-right] :upper
                                                          [lower-left lower-right] :lower
                                                          :as properties}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        bounding-box (:bounding-box meta)
        lower-left (assoc lower-left :x (:x upper-left))
        {line-upper :line
         line-upper-start :line-start
         line-upper-from :adjusted-from
         :as line-upper-data} (line/create-with-extension line
                                                          upper-left upper-right
                                                          bounding-box
                                                          :context context)
        {line-lower :line
         line-lower-start :line-start
         line-lower-to :adjusted-to
         :as line-lower-data} (line/create-with-extension opposite-line
                                                          lower-left lower-right
                                                          bounding-box
                                                          :reversed? true
                                                          :context context)]
    (post-process/shape
     {:shape [(path/make-path
               ["M" (v/add line-upper-from line-upper-start)
                (path/stitch line-upper)
                "L" (v/add line-lower-to line-lower-start)
                (path/stitch line-lower)
                "z"])]
      :lines [{:line line
               :line-from line-upper-from
               :line-data [line-upper-data]}
              {:line opposite-line
               :line-from line-lower-to
               :line-data [line-lower-data]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base flip-cottise? humetty]
                                                        [reference-upper-left reference-upper-right] :upper
                                                        [reference-lower-left reference-lower-right] :lower
                                                        reference-upper-line :line
                                                        reference-lower-line :opposite-line}]
  (let [kind (cottising/kind context)
        distance (interface/get-sanitized-data (c/++ context :distance))
        distance (math/percent-of percentage-base distance)
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        band-size (math/percent-of percentage-base thickness)
        line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                       line-length percentage-base)
        opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                line-length percentage-base)
        opposite? (or flip-cottise?
                      (-> kind name (s/starts-with? "cottise-opposite")))
        reference-line (if opposite?
                         reference-lower-line
                         reference-upper-line)
        real-distance (+ (:effective-height reference-line)
                         distance)
        [base-left base-right] (if opposite?
                                 [reference-lower-left reference-lower-right]
                                 [reference-upper-left reference-upper-right])
        dist-vector (v/Vector. 0 real-distance)
        band-size-vector (v/Vector. 0 band-size)
        add-fn (if opposite?
                 v/add
                 v/sub)
        [first-left first-right] (map #(add-fn % dist-vector) [base-left base-right])
        [second-left second-right] (map #(add-fn % band-size-vector) [first-left first-right])
        [upper-left upper-right
         lower-left lower-right] (if opposite?
                                   [first-left first-right
                                    second-left second-right]
                                   [second-left second-right
                                    first-left first-right])
        [line opposite-line] (if opposite?
                               [opposite-line line]
                               [line opposite-line])]
    (post-process/properties
     {:type ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :line line
      :opposite-line opposite-line
      :humetty humetty}
     context)))
