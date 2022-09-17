(ns heraldicon.heraldry.ordinary.type.bend
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
   [heraldicon.interface :as interface]
   [heraldicon.math.angle :as angle]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/bend)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/bend)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [parent-context (interface/parent context)
        {:keys [affected-paths]} (interface/get-auto-ordinary-info ordinary-type parent-context)
        auto-position-index (get affected-paths (:path context))
        auto-positioned? auto-position-index
        default-size (interface/get-sanitized-data (c/++ parent-context :bend-group :default-size))
        default-spacing (interface/get-sanitized-data (c/++ parent-context :bend-group :default-spacing))
        line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside)
                       (cond->
                         auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside)
                                (cond->
                                  auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        anchor-point-option {:type :option.type/choice
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
                                        :bottom-right])
                             :default :auto
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-anchor-point
                                              :top-left [:fess
                                                         :chief
                                                         :base
                                                         :honour
                                                         :nombril
                                                         :bottom-right
                                                         :hoist
                                                         :fly
                                                         :center
                                                         :angle]
                                              :bottom-right [:fess
                                                             :chief
                                                             :base
                                                             :honour
                                                             :nombril
                                                             :hoist
                                                             :fly
                                                             :top-left
                                                             :center
                                                             :angle]
                                              [:top-left
                                               :bottom-right
                                               :angle]))
                                  :default (case current-anchor-point
                                             :top-left :fess
                                             :bottom-right :fess
                                             :top-left)
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    (ordinary.shared/add-humetty-and-voided
     {:anchor (cond-> {:point anchor-point-option
                       :ui/label :string.option/anchor
                       :ui/element :ui.element/position}
                (and auto-positioned?
                     (pos? auto-position-index)) (assoc :spacing-bottom {:type :option.type/range
                                                                         :min -75
                                                                         :max 75
                                                                         :default default-spacing
                                                                         :ui/label :string.option/spacing-bottom
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
                           :angle) (assoc :offset-x {:type :option.type/range
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
                                                     :ui/step 0.1})

                     (and (not= current-orientation-point
                                :angle)
                          (not auto-positioned?)) (assoc :alignment {:type :option.type/choice
                                                                     :choices position/alignment-choices
                                                                     :default :middle
                                                                     :ui/label :string.option/alignment
                                                                     :ui/element :ui.element/radio-select}))
      :line line-style
      :opposite-line opposite-line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 90
                        :default (if auto-positioned?
                                   default-size
                                   25)
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2)} context)))

(defn- add-bend [{:keys [current-y]
                  :as arrangement}
                 {:keys [size
                         spacing-bottom
                         line
                         opposite-line
                         cottise-height
                         opposite-cottise-height]
                  :as bend}]
  (let [line-height (:effective-height line)
        opposite-line-height (:effective-height opposite-line)
        new-current-y (cond-> (- current-y
                                 cottise-height
                                 line-height
                                 size
                                 opposite-line-height
                                 opposite-cottise-height)
                        (not (zero? current-y)) (- spacing-bottom))]
    (-> arrangement
        (update :bends conj (assoc bend :anchor-point (v/Vector. 0 (+ new-current-y
                                                                      cottise-height
                                                                      line-height
                                                                      (/ size 2)))))
        (assoc :current-y new-current-y))))

(defmethod interface/auto-arrangement ordinary-type [_ordinary-type context]
  (let [{:keys [height width points]
         :as environment} (interface/get-environment context)
        {:keys [top-left]} points
        anchor {:point :top-left}
        bend-group-context (c/++ context :bend-group)
        orientation (interface/get-sanitized-data (c/++ bend-group-context :orientation))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               ;; no alignment, so size is not needed
                                               nil
                                               nil)
        bend-angle (angle/normalize (v/angle-to-point anchor-point orientation-point))
        rotated-shape (-> (interface/get-exact-shape context)
                          path/parse-path
                          (path/translate (v/mul top-left -1))
                          (path/rotate (- bend-angle))
                          path/to-svg)
        rotated-bounding-box (bb/from-paths [rotated-shape])
        start-x 0
        end-x (:max-x rotated-bounding-box)
        line-length (- end-x start-x)
        percentage-base (min width height)
        apply-percentage (partial math/percent-of percentage-base)
        {:keys [ordinary-contexts
                num-ordinaries]} (interface/get-auto-ordinary-info ordinary-type context)
        bends (when (> num-ordinaries 1)
                (let [{:keys [current-y
                              bends]} (->> ordinary-contexts
                                           (map (fn [context]
                                                  (-> {:context context
                                                       :start-x start-x
                                                       :end-x end-x
                                                       :line-length line-length
                                                       :bend-angle bend-angle
                                                       :percentage-base percentage-base}
                                                      auto-arrange/set-spacing-bottom
                                                      auto-arrange/set-size
                                                      auto-arrange/set-line-data
                                                      auto-arrange/set-cottise-data
                                                      (update :spacing-bottom apply-percentage)
                                                      (update :size apply-percentage))))
                                           (reduce add-bend {:current-y 0
                                                             :bends []}))
                      offset-y (interface/get-sanitized-data (c/++ bend-group-context :offset-y))
                      total-height (- current-y)
                      half-height (/ total-height 2)
                      start-y (- half-height)]
                  (map (fn [bend]
                         (update bend :anchor-point v/add (v/Vector. 0 (- start-y current-y offset-y))))
                       bends)))]
    {:arrangement-data (into {}
                             (map (fn [{:keys [context]
                                        :as bend}]
                                    [(:path context) bend]))
                             bends)
     :num-ordinaries num-ordinaries}))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [width height points]
         :as parent-environment} (interface/get-parent-environment context)
        {:keys [top-left]} points
        percentage-base (min width height)
        apply-percentage (partial math/percent-of percentage-base)
        {:keys [arrangement-data]} (interface/get-auto-arrangement ordinary-type (interface/parent context))
        {arranged-size :size
         arranged-anchor-point :anchor-point
         arranged-bend-angle :bend-angle
         arranged-start-x :start-x
         arranged-end-x :end-x} (get arrangement-data (:path context))
        arranged? arranged-size
        real-ordinary-type (interface/get-raw-data (c/++ context :type))
        sinister? (= real-ordinary-type
                     :heraldry.ordinary.type/bend-sinister)
        counterchanged? (= (interface/get-sanitized-data (c/++ context :field :type))
                           :heraldry.field.type/counterchanged)
        inherit-environment? (interface/get-sanitized-data (c/++ context :field :inherit-environment?))
        use-parent-environment? (or counterchanged?
                                    inherit-environment?)
        band-size (or arranged-size
                      (apply-percentage (interface/get-sanitized-data (c/++ context :geometry :size))))
        anchor (when-not arranged?
                 (interface/get-sanitized-data (c/++ context :anchor)))
        orientation (when-not arranged?
                      (interface/get-sanitized-data (c/++ context :orientation)))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (if arranged?
                                                (let [rotated-anchor-point (-> arranged-anchor-point
                                                                               (v/rotate arranged-bend-angle)
                                                                               (v/add top-left))]
                                                  {:real-anchor rotated-anchor-point
                                                   :real-orientation (-> (v/Vector. 1 0)
                                                                         (v/rotate arranged-bend-angle)
                                                                         (v/add rotated-anchor-point))})
                                                (position/calculate-anchor-and-orientation
                                                 parent-environment
                                                 anchor
                                                 orientation
                                                 band-size
                                                 nil))
        direction (v/sub orientation-point anchor-point)
        direction (-> (v/Vector. (-> direction :x Math/abs)
                                 (-> direction :y Math/abs))
                      v/normal
                      (cond->
                        sinister? (v/dot (v/Vector. 1 -1))))
        direction-orthogonal (v/orthogonal direction)
        direction-orthogonal (if (neg? (:y direction-orthogonal))
                               (v/mul direction-orthogonal -1)
                               direction-orthogonal)
        parent-shape (interface/get-exact-parent-shape context)
        [middle-start middle-end] (v/intersections-with-shape anchor-point (v/add anchor-point direction)
                                                              parent-shape :default? true)
        angle (or arranged-bend-angle
                  (v/angle-to-point middle-start middle-end))
        width-offset (-> direction-orthogonal
                         (v/mul band-size)
                         (v/div 2))
        upper-start (v/sub middle-start width-offset)
        upper-end (v/sub middle-end width-offset)
        lower-start (v/add middle-start width-offset)
        lower-end (v/add middle-end width-offset)
        [upper-left upper-right] (v/intersections-with-shape upper-start upper-end parent-shape :default? true)
        [lower-left lower-right] (v/intersections-with-shape lower-start lower-end parent-shape :default? true)
        rotated-upper-left (v/rotate upper-left (- angle))
        rotated-upper-right (v/rotate upper-right (- angle))
        rotated-middle-start (v/rotate middle-start (- angle))
        rotated-middle-end (v/rotate middle-end (- angle))
        rotated-lower-left (v/rotate lower-left (- angle))
        rotated-lower-right (v/rotate lower-right (- angle))
        start-x (or arranged-start-x
                    (apply min (map :x [rotated-upper-left
                                        rotated-middle-start
                                        rotated-lower-left])))
        end-x (or arranged-end-x
                  (apply max (map :x [rotated-upper-right
                                      rotated-middle-end
                                      rotated-lower-right])))
        upper-left (v/rotate (assoc rotated-upper-left :x start-x) angle)
        upper-right (v/rotate (assoc rotated-upper-right :x end-x) angle)
        lower-left (v/rotate (assoc rotated-lower-left :x start-x) angle)
        lower-right (v/rotate (assoc rotated-lower-right :x end-x) angle)
        line-length (max (- end-x start-x) 10)
        reverse-transform-fn (when-not use-parent-environment?
                               (fn reverse-transform-fn [v]
                                 (if (instance? v/Vector v)
                                   (-> v
                                       (v/sub upper-left)
                                       (v/rotate (- angle)))
                                   (-> v
                                       (path/translate (- (:x upper-left)) (- (:y upper-left)))
                                       (path/rotate (- angle))))))]
    (post-process/properties
     {:type real-ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :angle angle
      :direction-orthogonal direction-orthogonal
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :use-parent-environment? use-parent-environment?
      :transform (when-not use-parent-environment?
                   (str "translate(" (v/->str upper-left) ")"
                        "rotate(" angle ")"))
      :bounding-box-transform-fn (when-not use-parent-environment?
                                   #(-> %
                                        (bb/translate upper-left)
                                        (bb/rotate angle)))
      :reverse-transform-fn reverse-transform-fn
      :humetty-percentage-base (min (:width parent-environment)
                                    (:height parent-environment))
      :voided-percentage-base band-size}
     context)))

(defmethod interface/environment ordinary-type [_context {:keys [reverse-transform-fn]
                                                          [upper-left upper-right] :upper
                                                          [lower-left lower-right] :lower}]
  (let [;; TODO: this should be more accurate
        bounding-box-points (cond->> [upper-left upper-right
                                      lower-left lower-right]
                              reverse-transform-fn (map reverse-transform-fn))]
    (environment/create (bb/from-points bounding-box-points))))

(defmethod interface/bounding-box ordinary-type [context {:keys [bounding-box-transform-fn]}]
  (let [bounding-box-transform-fn (or bounding-box-transform-fn identity)]
    (some-> (interface/get-environment context)
            :bounding-box
            bounding-box-transform-fn)))

(defmethod interface/render-shape ordinary-type [context {:keys [line opposite-line]
                                                          [upper-left upper-right] :upper
                                                          [lower-left lower-right] :lower
                                                          :as properties}]
  (let [{:keys [bounding-box]} (interface/get-parent-environment context)
        line-upper (line/create-with-extension context
                                               line
                                               upper-left upper-right
                                               bounding-box)
        line-lower (line/create-with-extension context
                                               opposite-line
                                               lower-left lower-right
                                               bounding-box
                                               :reversed? true)]
    (post-process/shape
     {:shape [(shape/build-shape
               context
               line-upper
               :clockwise-shortest
               line-lower
               :clockwise-shortest)]
      :edges [{:lines [line-upper]}
              {:lines [line-lower]}]}
     context
     properties)))

(defmethod interface/exact-shape ordinary-type [context {:keys [reverse-transform-fn]}]
  (let [exact-shape (interface/fallback-exact-shape context)]
    (if reverse-transform-fn
      (-> exact-shape
          path/parse-path
          reverse-transform-fn
          path/to-svg)
      exact-shape)))

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base direction-orthogonal
                                                               angle flip-cottise? humetty]
                                                        real-ordinary-type :type
                                                        [reference-upper-left reference-upper-right] :upper
                                                        [reference-lower-left reference-lower-right] :lower
                                                        reference-upper-line :line
                                                        reference-lower-line :opposite-line}]
  (let [kind (cottising/kind context)
        distance (interface/get-sanitized-data (c/++ context :distance))
        distance (math/percent-of percentage-base distance)
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        band-size (math/percent-of percentage-base thickness)
        counterchanged? (= (interface/get-sanitized-data (c/++ context :field :type))
                           :heraldry.field.type/counterchanged)
        inherit-environment? (interface/get-sanitized-data (c/++ context :field :inherit-environment?))
        use-parent-environment? (or counterchanged?
                                    inherit-environment?)
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
        dist-vector (v/mul direction-orthogonal real-distance)
        band-size-vector (v/mul direction-orthogonal band-size)
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
        reverse-transform-fn (when-not use-parent-environment?
                               (fn reverse-transform-fn [v]
                                 (if (instance? v/Vector v)
                                   (-> v
                                       (v/sub upper-left)
                                       (v/rotate (- angle)))
                                   (-> v
                                       (path/translate (- (:x upper-left)) (- (:y upper-left)))
                                       (path/rotate (- angle))))))]
    (post-process/properties
     {:type real-ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :angle angle
      :direction-orthogonal direction-orthogonal
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :use-parent-environment? false
      :transform (when-not use-parent-environment?
                   (str "translate(" (v/->str upper-left) ")"
                        "rotate(" angle ")"))
      :bounding-box-transform-fn (when-not use-parent-environment?
                                   #(-> %
                                        (bb/translate upper-left)
                                        (bb/rotate angle)))
      :reverse-transform-fn reverse-transform-fn
      :swap-lines? opposite?
      :humetty humetty}
     context)))
