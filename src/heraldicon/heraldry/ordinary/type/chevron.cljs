(ns heraldicon.heraldry.ordinary.type.chevron
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.auto-arrange :as auto-arrange]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.post-process :as post-process]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.heraldry.shared.chevron :as chevron]
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/chevron)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/chevron)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [parent-context (interface/parent context)
        {:keys [affected-paths]} (interface/get-auto-ordinary-info ordinary-type parent-context)
        auto-position-index (get affected-paths (:path context))
        auto-positioned? auto-position-index
        default-size (interface/get-sanitized-data (c/++ parent-context :chevron-group :default-size))
        default-spacing (interface/get-sanitized-data (c/++ parent-context :chevron-group :default-spacing))
        line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside)
                       (cond->
                         auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside)
                                (cond->
                                  auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        origin-point-option (if auto-positioned?
                              {:type :option.type/choice
                               :choices (position/orientation-choices
                                         [:auto])
                               :default :auto
                               :ui/label :string.option/point}
                              {:type :option.type/choice
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
                               :ui/label :string.option/point})
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        orientation-point-option (if auto-positioned?
                                   {:type :option.type/choice
                                    :choices (position/orientation-choices
                                              [:auto])
                                    :default :auto
                                    :ui/label :string.option/point}
                                   {:type :option.type/choice
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
                                    :ui/label :string.option/point})
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:anchor (cond-> {:point {:type :option.type/choice
                               :choices (position/anchor-choices
                                         [:auto
                                          :fess
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
                               :default :auto
                               :ui/label :string.option/point}
                       :ui/label :string.option/anchor
                       :ui/element :ui.element/position}
                (and auto-positioned?
                     (pos? auto-position-index)) (assoc :spacing-top {:type :option.type/range
                                                                      :min -75
                                                                      :max 75
                                                                      :default default-spacing
                                                                      :ui/label :string.option/spacing-top
                                                                      :ui/step 0.1})
                (not auto-positioned?) (assoc :alignment {:type :option.type/choice
                                                          :choices position/alignment-choices
                                                          :default :middle
                                                          :ui/label :string.option/alignment
                                                          :ui/element :ui.element/radio-select}
                                              :offset-x {:type :option.type/range
                                                         :min -75
                                                         :max 75
                                                         :default 0
                                                         :ui/label :string.option/offset-x
                                                         :ui/step 0.1}
                                              :offset-y {:type :option.type/range
                                                         :min -75
                                                         :max 75
                                                         :default 0
                                                         :ui/label :string.option/offset-y
                                                         :ui/step 0.1}))
      :origin (cond-> {:point origin-point-option
                       :ui/label :string.charge.attitude/issuant
                       :ui/element :ui.element/position}

                (and (not auto-positioned?)
                     (= current-origin-point
                        :angle)) (assoc :angle {:type :option.type/range
                                                :min -180
                                                :max 180
                                                :default 0
                                                :ui/label :string.option/angle})

                (and (not auto-positioned?)
                     (not= current-origin-point
                           :angle)) (assoc :offset-x {:type :option.type/range
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

                     (and (not auto-positioned?)
                          (= current-orientation-point
                             :angle)) (assoc :angle {:type :option.type/range
                                                     :min 0
                                                     :max 360
                                                     :default 45
                                                     :ui/label :string.option/angle})

                     (and (not auto-positioned?)
                          (not= current-orientation-point
                                :angle)) (assoc :alignment {:type :option.type/choice
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
                        :default (if auto-positioned?
                                   default-size
                                   20)
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2 :size-reference-default (when auto-positioned?
                                                                              :field-width))}
     context)))

(defn- scale-factor [arm-angle]
  (/ 1 (-> arm-angle angle/to-rad Math/sin (or 1))))

(defn- add-chevron [{:keys [current-y]
                     :as arrangement}
                    {:keys [size
                            scale
                            spacing-top
                            line
                            opposite-line
                            cottise-height
                            opposite-cottise-height]
                     :as bar}]
  (let [line-height (:effective-height line)
        opposite-line-height (:effective-height opposite-line)
        new-current-y (cond-> (+ current-y
                                 (* scale
                                    (+ cottise-height
                                       line-height
                                       size
                                       opposite-line-height
                                       opposite-cottise-height)))
                        (not (zero? current-y)) (+ (* scale spacing-top)))]
    (-> arrangement
        (update :chevrons conj (assoc bar :anchor-point (v/Vector. 0 (- new-current-y
                                                                        (* scale
                                                                           (+ opposite-cottise-height
                                                                              opposite-line-height
                                                                              (/ size 2)))))))
        (assoc :current-y new-current-y))))

(defmethod interface/auto-arrangement ordinary-type [_ordinary-type context]
  (let [{:keys [width height]
         :as environment} (interface/get-environment context)
        percentage-base (min width height)
        apply-percentage (partial math/percent-of percentage-base)
        chevron-group-context (c/++ context :chevron-group)
        origin (interface/get-sanitized-data (c/++ chevron-group-context :origin))
        anchor {:point :fess}
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          environment
                                          anchor
                                          origin
                                          0
                                          90)
        chevron-angle (angle/normalize
                       (v/angle-to-point direction-anchor-point
                                         origin-point))
        parent-shape (interface/get-exact-parent-shape context)
        [upper-intersection lower-intersection] (v/intersections-with-shape
                                                 direction-anchor-point origin-point
                                                 parent-shape :default? true)
        min-y (- (v/abs (v/sub upper-intersection direction-anchor-point)))
        max-y (v/abs (v/sub lower-intersection direction-anchor-point))
        arm-angle (interface/get-sanitized-data (c/++ chevron-group-context :orientation :angle))
        scale-factor! (scale-factor arm-angle)
        {:keys [ordinary-contexts
                num-ordinaries
                default-spacing]} (interface/get-auto-ordinary-info ordinary-type context)
        chevrons (when (> num-ordinaries 1)
                   (let [{:keys [current-y
                                 chevrons]} (->> ordinary-contexts
                                                 (map (fn [context]
                                                        (-> {:context context
                                                             :line-length width
                                                             :scale scale-factor!
                                                             :percentage-base percentage-base}
                                                            auto-arrange/set-spacing-top
                                                            auto-arrange/set-size
                                                            auto-arrange/set-line-data
                                                            auto-arrange/set-cottise-data
                                                            (update :spacing-top apply-percentage)
                                                            (update :size apply-percentage))))
                                                 (reduce add-chevron {:current-y 0
                                                                      :chevrons []}))
                         offset-x (interface/get-sanitized-data (c/++ context :chevron-group :offset-x))
                         offset-y (interface/get-sanitized-data (c/++ context :chevron-group :offset-y))
                         relevant-height (- max-y min-y)
                         total-height current-y
                         half-height (/ total-height 2)
                         weight (min (* (/ total-height (* 0.8 relevant-height))
                                        (/ num-ordinaries
                                           (inc num-ordinaries))) 1)
                         adjusted-spacing (* scale-factor! default-spacing)
                         center-offset (/ (+ max-y min-y) 2)
                         middle-y (* weight center-offset)
                         start-y (if (> (+ total-height (* 2 adjusted-spacing))
                                        relevant-height)
                                   (- center-offset half-height)
                                   (-> (- middle-y half-height)
                                       (max (+ min-y adjusted-spacing))
                                       (min (- max-y adjusted-spacing total-height))))]
                     (map (fn [{:keys [anchor-point]
                                :as bar}]
                            (let [anchor-point (v/add anchor-point
                                                      (v/Vector. 0 start-y)
                                                      (v/Vector. offset-x (- offset-y)))
                                  arm-point (-> (v/Vector. 0 1)
                                                (v/rotate arm-angle)
                                                (v/add anchor-point))]
                              (-> bar
                                  (assoc :chevron-angle chevron-angle)
                                  (assoc :anchor-point (-> anchor-point
                                                           (v/rotate (- chevron-angle 90))
                                                           (v/add direction-anchor-point)))
                                  (assoc :arm-point (-> arm-point
                                                        (v/rotate (- chevron-angle 90))
                                                        (v/add direction-anchor-point))))))
                          chevrons)))]
    {:arrangement-data (into {}
                             (map (fn [{:keys [context]
                                        :as bar}]
                                    [(:path context) bar]))
                             chevrons)
     :num-ordinaries num-ordinaries}))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height]
         :as parent-environment} (interface/get-parent-environment context)
        percentage-base (min width height)
        apply-percentage (partial math/percent-of percentage-base)
        {:keys [arrangement-data]} (interface/get-auto-arrangement ordinary-type (interface/parent context))
        {arranged-size :size
         arranged-anchor-point :anchor-point
         arranged-arm-point :arm-point
         arranged-chevron-angle :chevron-angle} (get arrangement-data (:path context))
        arranged? arranged-size
        band-size (or arranged-size
                      (apply-percentage (interface/get-sanitized-data (c/++ context :geometry :size))))
        anchor (when-not arranged?
                 (interface/get-sanitized-data (c/++ context :anchor)))
        orientation (when-not arranged?
                      (interface/get-sanitized-data (c/++ context :orientation)))
        origin (when-not arranged?
                 (interface/get-sanitized-data (c/++ context :origin)))
        origin (when-not arranged?
                 (update origin :point (fn [origin-point]
                                         (get {:chief :top
                                               :base :bottom
                                               :dexter :left
                                               :sinister :right} origin-point origin-point))))
        raw-origin (when-not arranged?
                     (interface/get-raw-data (c/++ context :origin)))
        origin (when-not arranged?
                 (cond-> origin
                   (-> origin
                       :point
                       #{:left
                         :right
                         :top
                         :bottom}) (assoc :offset-x (or (:offset-x raw-origin)
                                                        (:offset-x anchor))
                                          :offset-y (or (:offset-y raw-origin)
                                                        (:offset-y anchor)))))
        unadjusted-anchor-point (when-not arranged?
                                  (position/calculate anchor parent-environment))
        {direction-anchor-point :real-anchor
         origin-point :real-orientation} (position/calculate-anchor-and-orientation
                                          parent-environment
                                          anchor
                                          origin
                                          0
                                          90)
        chevron-angle (or arranged-chevron-angle
                          (angle/normalize
                           (v/angle-to-point direction-anchor-point
                                             origin-point)))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (when-not arranged?
                                                (position/calculate-anchor-and-orientation
                                                 parent-environment
                                                 anchor
                                                 orientation
                                                 band-size
                                                 chevron-angle))
        [mirrored-anchor mirrored-orientation] (when-not arranged?
                                                 [(chevron/mirror-point chevron-angle unadjusted-anchor-point anchor-point)
                                                  (chevron/mirror-point chevron-angle unadjusted-anchor-point orientation-point)])
        anchor-point (or arranged-anchor-point
                         (v/line-intersection anchor-point orientation-point
                                              mirrored-anchor mirrored-orientation))
        orientation-point (or arranged-arm-point
                              orientation-point)
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
                         (apply max))]
    (post-process/properties
     {:type ordinary-type
      :upper [intersection-upper-left upper-corner intersection-upper-right]
      :lower [intersection-lower-left lower-corner intersection-lower-right]
      :chevron-angle chevron-angle
      :joint-angle joint-angle
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :humetty-percentage-base (min (:width parent-environment)
                                    (:height parent-environment))
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [_context {[upper-left upper-corner upper-right] :upper
                                                          [lower-left lower-corner lower-right] :lower}]
  (let [;; TODO: needs to be improved
        bounding-box-points [upper-corner lower-corner
                             upper-left upper-right
                             lower-left lower-right]]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line]
                                                          [upper-left upper-corner upper-right] :upper
                                                          [lower-left lower-corner lower-right] :lower
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        line (dissoc line :base-line)
        opposite-line (dissoc opposite-line :base-line)
        line-upper-left (line/create-with-extension context
                                                    line
                                                    upper-corner upper-left
                                                    bounding-box
                                                    :reversed? true
                                                    :extend-from? false)
        line-upper-right (line/create-with-extension context
                                                     line
                                                     upper-corner upper-right
                                                     bounding-box
                                                     :extend-from? false)
        line-lower-right (line/create-with-extension context
                                                     opposite-line
                                                     lower-corner lower-right
                                                     bounding-box
                                                     :reversed? true
                                                     :extend-from? false)
        line-lower-left (line/create-with-extension context
                                                    opposite-line
                                                    lower-corner lower-left
                                                    bounding-box
                                                    :extend-from? false)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-upper-left
               line-upper-right
               :clockwise-shortest
               line-lower-right
               line-lower-left
               :clockwise-shortest)]
      :edges [{:lines [line-upper-left line-upper-right]}
              {:lines [line-lower-right line-lower-left]}]}
     context
     properties)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base
                                                               chevron-angle joint-angle flip-cottise?
                                                               humetty]
                                                        [reference-upper-left reference-upper-corner reference-upper-right] :upper
                                                        [reference-lower-left reference-lower-corner reference-lower-right] :lower
                                                        reference-upper-line :line
                                                        reference-lower-line :opposite-line}]
  (let [kind (cottising/kind context)
        distance (interface/get-sanitized-data (c/++ context :distance))
        distance (math/percent-of percentage-base distance)
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        band-size (math/percent-of percentage-base thickness)
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
                                    first-left first-right])]
    (post-process/properties
     {:type ordinary-type
      :upper [upper-left upper-corner upper-right]
      :lower [lower-left lower-corner lower-right]
      :chevron-angle chevron-angle
      :joint-angle joint-angle
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :swap-lines? opposite?
      :humetty humetty}
     context)))
