(ns heraldicon.heraldry.ordinary.type.chevron
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.render :as ordinary.render]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.heraldry.shared.chevron :as chevron]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/chevron)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/chevron)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        origin-point-option {:type :option.type/choice
                             :choices (position/orientation-choices
                                       [:chief
                                        :base
                                        :dexter
                                        :sinister
                                        :hoist
                                        :fly
                                        :top-left
                                        :top-right
                                        :bottom-left
                                        :bottom-right
                                        :angle])
                             :default :base
                             :ui/label :string.option/point}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-origin-point
                                              :base [:bottom-left
                                                     :bottom-right
                                                     :left
                                                     :right
                                                     :angle]
                                              :chief [:top-left
                                                      :top-right
                                                      :left
                                                      :right
                                                      :angle]
                                              :dexter [:top-left
                                                       :bottom-left
                                                       :top
                                                       :bottom
                                                       :angle]
                                              :sinister [:top-right
                                                         :bottom-right
                                                         :top
                                                         :bottom
                                                         :angle]
                                              :bottom-left [:bottom
                                                            :bottom-right
                                                            :top-left
                                                            :left
                                                            :angle]
                                              :bottom-right [:bottom-left
                                                             :bottom
                                                             :right
                                                             :top-right
                                                             :angle]
                                              :top-left [:top
                                                         :top-right
                                                         :left
                                                         :bottom-left
                                                         :angle]
                                              :top-right [:top-left
                                                          :top
                                                          :right
                                                          :bottom-right
                                                          :angle]
                                              [:top-left
                                               :top
                                               :top-right
                                               :left
                                               :right
                                               :bottom-left
                                               :bottom
                                               :bottom-right
                                               :angle]))
                                  :default (case current-origin-point
                                             :base :bottom-left
                                             :chief :top-right
                                             :dexter :top-left
                                             :sinister :bottom-right
                                             :bottom-left :left
                                             :bottom-right :right
                                             :top-left :left
                                             :top-right :right
                                             :angle :angle
                                             :bottom-left)
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
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
                                  :top-left
                                  :top
                                  :top-right
                                  :left
                                  :center
                                  :right
                                  :bottom-left
                                  :bottom
                                  :bottom-right])
                       :default :fess
                       :ui/label :string.option/point}
               :alignment {:type :option.type/choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui/label :string.option/alignment
                           :ui/element :ui.element/radio-select}
               :offset-x {:type :option.type/range
                          :min -50
                          :max 50
                          :default 0
                          :ui/label :string.option/offset-x
                          :ui/step 0.1}
               :offset-y {:type :option.type/range
                          :min -75
                          :max 75
                          :default 0
                          :ui/label :string.option/offset-y
                          :ui/step 0.1}
               :ui/label :string.option/anchor
               :ui/element :ui.element/position}
      :origin (cond-> {:point origin-point-option
                       :ui/label :string.charge.attitude/issuant
                       :ui/element :ui.element/position}

                (= current-origin-point
                   :angle) (assoc :angle {:type :option.type/range
                                          :min -180
                                          :max 180
                                          :default 0
                                          :ui/label :string.option/angle})

                (not= current-origin-point
                      :angle) (assoc :offset-x {:type :option.type/range
                                                :min -50
                                                :max 50
                                                :default 0
                                                :ui/label :string.option/offset-x
                                                :ui/step 0.1}
                                     :offset-y {:type :option.type/range
                                                :min -75
                                                :max 75
                                                :default 0
                                                :ui/label :string.option/offset-y
                                                :ui/step 0.1}))
      :orientation (cond-> {:point orientation-point-option
                            :ui/label :string.option/orientation
                            :ui/element :ui.element/position}

                     (= current-orientation-point
                        :angle) (assoc :angle {:type :option.type/range
                                               :min 0
                                               :max 360
                                               :default 45
                                               :ui/label :string.option/angle})

                     (not= current-orientation-point
                           :angle) (assoc :alignment {:type :option.type/choice
                                                      :choices position/alignment-choices
                                                      :default :middle
                                                      :ui/label :string.option/alignment
                                                      :ui/element :ui.element/radio-select}
                                          :offset-x {:type :option.type/range
                                                     :min -50
                                                     :max 50
                                                     :default 0
                                                     :ui/label :string.option/offset-x
                                                     :ui/step 0.1}
                                          :offset-y {:type :option.type/range
                                                     :min -75
                                                     :max 75
                                                     :default 0
                                                     :ui/label :string.option/offset-y
                                                     :ui/step 0.1}))
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
        percentage-base (:height parent-environment)
        band-size (math/percent-of percentage-base size)
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        origin (interface/get-sanitized-data (c/++ context :origin))
        origin (update origin :point (fn [origin-point]
                                       (get {:chief :top
                                             :base :bottom
                                             :dexter :left
                                             :sinister :right} origin-point origin-point)))
        raw-origin (interface/get-raw-data (c/++ context :origin))
        origin (cond-> origin
                 (-> origin
                     :point
                     #{:left
                       :right
                       :top
                       :bottom}) (assoc :offset-x (or (:offset-x raw-origin)
                                                      (:offset-x anchor))
                                        :offset-y (or (:offset-y raw-origin)
                                                      (:offset-y anchor))))
        unadjusted-anchor-point (position/calculate anchor parent-environment)
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          parent-environment
                                          anchor
                                          origin
                                          0
                                          90)
        chevron-angle (angle/normalize
                       (v/angle-to-point direction-anchor-point
                                         origin-point))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               parent-environment
                                               anchor
                                               orientation
                                               band-size
                                               chevron-angle)
        [mirrored-anchor mirrored-orientation] [(chevron/mirror-point chevron-angle unadjusted-anchor-point anchor-point)
                                                (chevron/mirror-point chevron-angle unadjusted-anchor-point orientation-point)]
        anchor-point (v/line-intersection anchor-point orientation-point
                                          mirrored-anchor mirrored-orientation)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle anchor-point orientation-point)
        diagonal-left (v/add anchor-point relative-left)
        diagonal-right (v/add anchor-point relative-right)
        angle-left (angle/normalize (v/angle-to-point anchor-point diagonal-left))
        angle-right (angle/normalize (v/angle-to-point anchor-point diagonal-right))
        joint-angle (angle/normalize (- angle-left angle-right))
        delta (/ band-size 2 (Math/sin (-> joint-angle
                                           (* Math/PI)
                                           (/ 180)
                                           (/ 2))))
        offset-lower (v/rotate
                      (v/Vector. delta 0)
                      chevron-angle)
        offset-upper (v/rotate
                      (v/Vector. (- delta) 0)
                      chevron-angle)
        upper-corner (v/add anchor-point offset-upper)
        lower-corner (v/add anchor-point offset-lower)
        upper-left (v/add diagonal-left offset-upper)
        lower-left (v/add diagonal-left offset-lower)
        upper-right (v/add diagonal-right offset-upper)
        lower-right (v/add diagonal-right offset-lower)
        parent-shape (interface/get-exact-parent-shape context)
        intersection-upper-left (v/last-intersection-with-shape upper-corner upper-left parent-shape :default? true)
        intersection-upper-right (v/last-intersection-with-shape upper-corner upper-right parent-shape :default? true)
        intersection-lower-left (v/last-intersection-with-shape lower-corner lower-left parent-shape :default? true)
        intersection-lower-right (v/last-intersection-with-shape lower-corner lower-right parent-shape :default? true)
        line-length (->> (concat (map (fn [v]
                                        (v/sub v lower-corner))
                                      [intersection-lower-left
                                       intersection-lower-right])
                                 (map (fn [v]
                                        (v/sub v upper-corner))
                                      [intersection-upper-left
                                       intersection-upper-right]))
                         (map v/abs)
                         (apply max))
        line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :line))
                                       line-length percentage-base)
        opposite-line (line/resolve-percentages (interface/get-sanitized-data (c/++ context :opposite-line))
                                                line-length percentage-base)]
    {:type ordinary-type
     :upper [intersection-upper-left upper-corner intersection-upper-right]
     :lower [intersection-lower-left lower-corner intersection-lower-right]
     :chevron-angle chevron-angle
     :joint-angle joint-angle
     :band-size band-size
     :line-length line-length
     :percentage-base percentage-base
     :line line
     :opposite-line opposite-line}))

(defmethod interface/environment ordinary-type [context {[upper-left upper-corner upper-right] :upper
                                                         [lower-left lower-corner lower-right] :lower}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        ;; TODO: needs to be improved
        bounding-box-points [upper-corner lower-corner
                             upper-left upper-right
                             lower-left lower-right]]
    (environment/create
     {:paths nil}
     (-> meta
         (dissoc :context)
         (merge {:bounding-box (bb/from-points bounding-box-points)})))))

(defmethod interface/render-shape ordinary-type [context {:keys [band-size line opposite-line]
                                                          [upper-left upper-corner upper-right] :upper
                                                          [lower-left lower-corner lower-right] :lower}]
  (let [{:keys [meta]} (interface/get-parent-environment context)
        {:keys [width]} (interface/get-environment context)
        bounding-box (:bounding-box meta)
        line (dissoc line :base-line)
        opposite-line (dissoc opposite-line :base-line)
        {line-upper-left :line
         line-upper-left-start :line-start
         line-upper-left-to :adjusted-to
         :as line-upper-left-data} (line/create-with-extension line
                                                               upper-corner upper-left
                                                               bounding-box
                                                               :reversed? true
                                                               :extend-from? false
                                                               :context context)
        {line-upper-right :line
         :as line-upper-right-data} (line/create-with-extension line
                                                                upper-corner upper-right
                                                                bounding-box
                                                                :extend-from? false
                                                                :context context)
        {line-lower-right :line
         line-lower-right-start :line-start
         line-lower-right-to :adjusted-to
         :as line-lower-right-data} (line/create-with-extension opposite-line
                                                                lower-corner lower-right
                                                                bounding-box
                                                                :reversed? true
                                                                :extend-from? false
                                                                :context context)
        {line-lower-left :line
         :as line-lower-left-data} (line/create-with-extension opposite-line
                                                               lower-corner lower-left
                                                               bounding-box
                                                               :extend-from? false
                                                               :context context)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add line-upper-left-to
                           line-upper-left-start)
                (path/stitch line-upper-left)
                (path/stitch line-upper-right)
                "L" (v/add line-lower-right-to
                           line-lower-right-start)
                (path/stitch line-lower-right)
                (path/stitch line-lower-left)
                "z"]
               width
               band-size
               context)]
    {:shape shape
     :lines [{:line line
              :line-from line-upper-left-to
              :line-data [line-upper-left-data line-upper-right-data]}
             {:line opposite-line
              :line-from line-lower-right-to
              :line-data [line-lower-right-data line-lower-left-data]}]}))

(defmethod ordinary.interface/render-ordinary ordinary-type [context]
  (ordinary.render/render context))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base
                                                               chevron-angle joint-angle flip-cottise?]
                                                        [reference-upper-left reference-upper-corner reference-upper-right] :upper
                                                        [reference-lower-left reference-lower-corner reference-lower-right] :lower
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
        [base-corner base-left base-right] (if opposite?
                                             [reference-lower-corner reference-lower-left reference-lower-right]
                                             [reference-upper-corner reference-upper-left reference-upper-right])
        real-distance (/ (+ (:effective-height reference-line)
                            distance)
                         (Math/sin (-> joint-angle
                                       (* Math/PI)
                                       (/ 180)
                                       (/ 2))))
        delta (/ band-size
                 (Math/sin (-> joint-angle
                               (* Math/PI)
                               (/ 180)
                               (/ 2))))
        dist-vector (v/rotate
                     (v/Vector. real-distance 0)
                     chevron-angle)
        band-size-vector (v/rotate
                          (v/Vector. delta 0)
                          chevron-angle)
        add-fn (if opposite?
                 v/add
                 v/sub)
        lower-corner (add-fn base-corner dist-vector)
        upper-corner (add-fn lower-corner band-size-vector)
        [first-left first-right] (map #(add-fn % dist-vector) [base-left base-right])
        [second-left second-right] (map #(add-fn % band-size-vector) [first-left first-right])
        [upper-corner lower-corner
         upper-left upper-right
         lower-left lower-right] (if opposite?
                                   [lower-corner upper-corner
                                    first-left first-right
                                    second-left second-right]
                                   [upper-corner lower-corner
                                    second-left second-right
                                    first-left first-right])
        [line opposite-line] (if opposite?
                               [opposite-line line]
                               [line opposite-line])]
    {:type ordinary-type
     :upper [upper-left upper-corner upper-right]
     :lower [lower-left lower-corner lower-right]
     :chevron-angle chevron-angle
     :joint-angle joint-angle
     :band-size band-size
     :line-length line-length
     :percentage-base percentage-base
     :line line
     :opposite-line opposite-line}))
