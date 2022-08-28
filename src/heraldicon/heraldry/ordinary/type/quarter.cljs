(ns heraldicon.heraldry.ordinary.type.quarter
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
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/quarter)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/quarter)

(def ^:private variant-choices
  [[:string.option.variant-quarter-choice/dexter-chief :dexter-chief]
   [:string.option.variant-quarter-choice/sinister-chief :sinister-chief]
   [:string.option.variant-quarter-choice/dexter-base :dexter-base]
   [:string.option.variant-quarter-choice/sinister-base :sinister-base]])

(def variant-map
  (options/choices->map variant-choices))

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:fess
                                  :chief
                                  :base
                                  :honour
                                  :nombril
                                  :hoist
                                  :fly
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
                          :min -45
                          :max 45
                          :default 0
                          :ui/label :string.option/offset-y
                          :ui/step 0.1}
               :ui/label :string.option/anchor
               :ui/element :ui.element/position}
      :line line-style
      :opposite-line opposite-line-style
      :variant {:type :option.type/choice
                :choices variant-choices
                :default :dexter-chief
                :ui/label :string.option/variant
                :ui/element :ui.element/select}
      :geometry {:size {:type :option.type/range
                        :min 10
                        :max 150
                        :default 100
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod interface/properties ordinary-type [context]
  (let [parent-environment (interface/get-parent-environment context)
        variant (interface/get-sanitized-data (c/++ context :variant))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        {:keys [top-left top-right
                bottom-left bottom-right]} (:points parent-environment)
        anchor-point (position/calculate anchor parent-environment :fess)
        percentage-base (:width parent-environment)
        {:keys [corner-point
                first-dir
                second-dir]} (case variant
                               :dexter-chief {:corner-point top-left
                                              :first-dir (v/Vector. 0 -1)
                                              :second-dir (v/Vector. -1 0)}
                               :sinister-chief {:corner-point top-right
                                                :first-dir (v/Vector. 1 0)
                                                :second-dir (v/Vector. 0 -1)}
                               :dexter-base {:corner-point bottom-left
                                             :first-dir (v/Vector. -1 0)
                                             :second-dir (v/Vector. 0 1)}
                               :sinister-base {:corner-point bottom-right
                                               :first-dir (v/Vector. 0 1)
                                               :second-dir (v/Vector. 1 0)})
        anchor-point (-> anchor-point
                         (v/sub corner-point)
                         (v/mul (/ size 100))
                         (v/add corner-point))
        parent-shape (interface/get-exact-parent-shape context)
        first-point (v/last-intersection-with-shape
                     anchor-point
                     (v/add anchor-point (v/mul first-dir 50))
                     parent-shape :default? true)
        second-point (v/last-intersection-with-shape
                      anchor-point
                      (v/add anchor-point (v/mul second-dir 50))
                      parent-shape :default? true)
        line-length (apply max (map (fn [v]
                                      (-> v
                                          (v/sub anchor-point)
                                          v/abs))
                                    [first-point
                                     second-point]))
        x-size (case variant
                 :dexter-chief (-> (v/sub anchor-point second-point) :y Math/abs)
                 :sinister-chief (-> (v/sub anchor-point first-point) :y Math/abs)
                 :dexter-base (-> (v/sub anchor-point first-point) :y Math/abs)
                 :sinister-base (-> (v/sub anchor-point second-point) :y Math/abs))]
    (post-process/properties
     {:type ordinary-type
      :edge [first-point anchor-point second-point]
      :corner-point corner-point
      :x-size x-size
      :variant variant
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (:width parent-environment)
      :voided-percentage-base x-size}
     context)))

(defmethod interface/environment ordinary-type [_context {:keys [corner-point]
                                                          [first-point anchor-point second-point] :edge}]
  (let [bounding-box-points [corner-point
                             first-point anchor-point second-point]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line]
                                                          [first-point anchor-point second-point] :edge
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        {line-one :line
         line-one-start :line-start
         line-one-to :adjusted-to
         :as line-one-data} (line/create-with-extension line
                                                        anchor-point first-point
                                                        bounding-box
                                                        :reversed? true
                                                        :extend-from? false
                                                        :context context)
        {line-two :line
         line-two-to :adjusted-to
         :as line-two-data} (line/create-with-extension opposite-line
                                                        anchor-point second-point
                                                        bounding-box
                                                        :extend-from? false
                                                        :context context)]
    (post-process/shape
     {:shape [(path/make-path
               ["M" (v/add line-one-to line-one-start)
                (path/stitch line-one)
                (path/stitch line-two)
                (infinity/clockwise line-two-to line-one-to)
                "z"])]
      :lines [{:line line
               :line-from line-one-to
               :line-data [line-one-data line-two-data]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base humetty]
                                                        [reference-first reference-point reference-second] :edge
                                                        variant :variant
                                                        reference-line :line}]
  (when-not (-> (cottising/kind context) name (s/starts-with? "cottise-opposite"))
    (let [distance (interface/get-sanitized-data (c/++ context :distance))
          distance (math/percent-of percentage-base distance)
          thickness (interface/get-sanitized-data (c/++ context :thickness))
          band-size (math/percent-of percentage-base thickness)
          line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                         line-length percentage-base)
          opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                  line-length percentage-base)
          [base-corner base-left base-right] [reference-point reference-first reference-second]
          joint-angle 90
          real-distance (/ (+ (:effective-height reference-line)
                              distance)
                           (Math/sin (-> joint-angle
                                         (* Math/PI)
                                         (/ 180)
                                         (/ 2))))
          quarter-angle (case variant
                          :dexter-chief 225
                          :sinister-chief 315
                          :dexter-base 135
                          :sinister-base 45)
          delta (/ band-size
                   (Math/sin (-> joint-angle
                                 (* Math/PI)
                                 (/ 180)
                                 (/ 2))))
          dist-vector (v/rotate
                       (v/Vector. real-distance 0)
                       quarter-angle)
          band-size-vector (v/rotate
                            (v/Vector. delta 0)
                            quarter-angle)
          lower-corner (v/sub base-corner dist-vector)
          upper-corner (v/sub lower-corner band-size-vector)
          [lower-left lower-right] (map #(v/sub % dist-vector) [base-left base-right])
          [upper-left upper-right] (map #(v/sub % band-size-vector) [lower-left lower-right])]
      (post-process/properties
       {:type :heraldry.ordinary.type/chevron
        :upper [upper-left upper-corner upper-right]
        :lower [lower-left lower-corner lower-right]
        :chevron-angle quarter-angle
        :joint-angle joint-angle
        :band-size band-size
        :line-length line-length
        :percentage-base percentage-base
        :line line
        :opposite-line opposite-line
        :humetty humetty}
       context))))
