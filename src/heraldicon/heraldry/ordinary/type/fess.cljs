(ns heraldicon.heraldry.ordinary.type.fess
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
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.shape :as shape]))

(def ordinary-type :heraldry.ordinary.type/fess)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/fess)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [parent-context (interface/parent context)
        {:keys [affected-paths]} (interface/get-auto-ordinary-info ordinary-type parent-context)
        auto-position-index (get affected-paths (:path context))
        auto-positioned? auto-position-index
        line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside)
                       (cond->
                         auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside)
                                (cond->
                                  auto-positioned? (options/override-if-exists [:size-reference :default] :field-width)))
        default-size (interface/get-sanitized-data (c/++ parent-context :fess-group :default-size))
        default-spacing (interface/get-sanitized-data (c/++ parent-context :fess-group :default-spacing))]
    (ordinary.shared/add-humetty-and-voided
     {:anchor (cond-> {:point {:type :option.type/choice
                               :choices (position/anchor-choices
                                         [:auto
                                          :fess
                                          :chief
                                          :base
                                          :honour
                                          :nombril
                                          :top
                                          :center
                                          :bottom])
                               :default :auto
                               :ui/label :string.option/point}

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
                                   25)
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2)} context)))

(defn- add-bar [{:keys [current-y]
                 :as arrangement}
                {:keys [size
                        spacing-bottom
                        line
                        opposite-line
                        cottise-height
                        opposite-cottise-height]
                 :as bar}]
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
        (update :bars conj (assoc bar :y (+ new-current-y
                                            cottise-height
                                            line-height
                                            (/ size 2))))
        (assoc :current-y new-current-y))))

(defmethod interface/auto-arrangement ordinary-type [_ordinary-type context]
  (let [{:keys [width points height]
         :as environment} (interface/get-environment context)
        {fess-y :y} (position/calculate {:point :fess} environment :fess)
        {center-y :y} (position/calculate {:point :center} environment :fess)
        start-x (-> points :left :x)
        min-y (-> points :top :y)
        max-y (-> points :bottom :y)
        percentage-base height
        apply-percentage (partial math/percent-of percentage-base)
        {:keys [ordinary-contexts
                num-ordinaries
                default-spacing]} (interface/get-auto-ordinary-info ordinary-type context)
        bars (when (> num-ordinaries 1)
               (let [{:keys [current-y
                             bars]} (->> ordinary-contexts
                                         (map (fn [context]
                                                {:context context}))
                                         (map #(assoc % :start-x start-x))
                                         (map #(assoc % :line-length width))
                                         (map auto-arrange/set-spacing-bottom)
                                         (map #(update % :spacing-bottom apply-percentage))
                                         (map auto-arrange/set-size)
                                         (map #(update % :size apply-percentage))
                                         (map auto-arrange/set-line-data)
                                         (map auto-arrange/set-cottise-data)
                                         (reduce add-bar {:current-y 0
                                                          :bars []}))
                     offset-y (interface/get-sanitized-data (c/++ context :fess-group :offset-y))
                     total-height (- current-y)
                     half-height (/ total-height 2)
                     weight (min (* (/ total-height (* 0.66666 height))
                                    (/ num-ordinaries
                                       (inc num-ordinaries))) 1)
                     middle-y (+ fess-y
                                 (* (- center-y fess-y)
                                    weight))
                     start-y (if (> (+ total-height (* 2 default-spacing))
                                    height)
                               (- center-y half-height)
                               (-> (- middle-y half-height)
                                   (max (+ min-y default-spacing))
                                   (min (- max-y default-spacing total-height))))]
                 (map (fn [bar]
                        (-> bar
                            (update :y - current-y offset-y)
                            (update :y + start-y)))
                      bars)))]
    {:arrangement-data (into {}
                             (map (fn [{:keys [context]
                                        :as bar}]
                                    [(:path context) bar]))
                             bars)
     :num-ordinaries num-ordinaries}))

(defmethod interface/properties ordinary-type [context]
  (let [{:keys [points height]
         :as parent-environment} (interface/get-parent-environment context)
        {:keys [left right]} points
        percentage-base height
        apply-percentage (partial math/percent-of percentage-base)
        {:keys [arrangement-data]} (interface/get-auto-arrangement ordinary-type (interface/parent context))
        {arranged-size :size
         arranged-y :y
         arranged-start-x :start-x
         arranged-line-length :line-length} (get arrangement-data (:path context))
        band-size (or arranged-size
                      (apply-percentage (interface/get-sanitized-data (c/++ context :geometry :size))))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        anchor-point (position/calculate anchor parent-environment :fess)
        upper (or (some-> arranged-y
                          (- (/ band-size 2)))
                  (case (:alignment anchor)
                    :left (:y anchor-point)
                    :right (- (:y anchor-point) band-size)
                    (- (:y anchor-point) (/ band-size 2))))
        lower (+ upper band-size)
        parent-shape (interface/get-exact-parent-shape context)
        [upper-left upper-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) upper) (v/Vector. (:x right) upper)
                                  parent-shape :default? true)
        [lower-left lower-right] (v/intersections-with-shape
                                  (v/Vector. (:x left) lower) (v/Vector. (:x right) lower)
                                  parent-shape :default? true)
        start-x (or arranged-start-x
                    (min (:x upper-left) (:x lower-left)))
        upper-left (assoc upper-left :x start-x)
        lower-left (assoc lower-left :x start-x)
        line-length (or arranged-line-length
                        (max (v/abs (v/sub upper-left upper-right))
                             (v/abs (v/sub lower-left lower-right))))]
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

(defmethod interface/environment ordinary-type [_context {[upper-left upper-right] :upper
                                                          [lower-left lower-right] :lower}]
  (let [bounding-box-points [upper-left upper-right
                             lower-left lower-right]]
    (environment/create (bb/from-points bounding-box-points))))

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

(defmethod cottising/cottise-properties ordinary-type [context
                                                       {:keys [line-length percentage-base flip-cottise? humetty]
                                                        [reference-upper-left reference-upper-right] :upper
                                                        [reference-lower-left reference-lower-right] :lower
                                                        reference-upper-line :line
                                                        reference-lower-line :opposite-line}]
  (let [kind (cottising/kind context)
        opposite? (or flip-cottise?
                      (-> kind name (s/starts-with? "cottise-opposite")))
        reference-line (if opposite?
                         reference-lower-line
                         reference-upper-line)
        [base-left base-right] (if opposite?
                                 [reference-lower-left reference-lower-right]
                                 [reference-upper-left reference-upper-right])
        direction (v/Vector. 0 1)
        distance (math/percent-of percentage-base
                                  (+ (:effective-height reference-line)
                                     (interface/get-sanitized-data (c/++ context :distance))))
        dist-vector (v/mul direction distance)
        band-size (math/percent-of percentage-base
                                   (interface/get-sanitized-data (c/++ context :thickness)))
        band-size-vector (v/mul direction band-size)
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
                                    first-left first-right])]
    (post-process/properties
     {:type ordinary-type
      :upper [upper-left upper-right]
      :lower [lower-left lower-right]
      :band-size band-size
      :line-length line-length
      :percentage-base percentage-base
      :swap-lines? opposite?
      :humetty humetty}
     context)))
