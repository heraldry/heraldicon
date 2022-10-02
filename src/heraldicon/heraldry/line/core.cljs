(ns heraldicon.heraldry.line.core
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.line.type.angled :as angled]
   [heraldicon.heraldry.line.type.bevilled :as bevilled]
   [heraldicon.heraldry.line.type.dancetty :as dancetty]
   [heraldicon.heraldry.line.type.dovetailed :as dovetailed]
   [heraldicon.heraldry.line.type.embattled :as embattled]
   [heraldicon.heraldry.line.type.embattled-grady :as embattled-grady]
   [heraldicon.heraldry.line.type.embattled-in-crosses :as embattled-in-crosses]
   [heraldicon.heraldry.line.type.enarched :as enarched]
   [heraldicon.heraldry.line.type.engrailed :as engrailed]
   [heraldicon.heraldry.line.type.fir-tree-topped :as fir-tree-topped]
   [heraldicon.heraldry.line.type.fir-twigged :as fir-twigged]
   [heraldicon.heraldry.line.type.indented :as indented]
   [heraldicon.heraldry.line.type.invected :as invected]
   [heraldicon.heraldry.line.type.nebuly :as nebuly]
   [heraldicon.heraldry.line.type.potenty :as potenty]
   [heraldicon.heraldry.line.type.raguly :as raguly]
   [heraldicon.heraldry.line.type.rayonny-flaming :as rayonny-flaming]
   [heraldicon.heraldry.line.type.rayonny-spiked :as rayonny-spiked]
   [heraldicon.heraldry.line.type.straight :as straight]
   [heraldicon.heraldry.line.type.thorny :as thorny]
   [heraldicon.heraldry.line.type.urdy :as urdy]
   [heraldicon.heraldry.line.type.wavy :as wavy]
   [heraldicon.heraldry.line.type.wolf-toothed :as wolf-toothed]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.path :as path]
   [heraldicon.svg.squiggly :as squiggly]
   [heraldicon.util.core :as util]
   [heraldicon.util.uid :as uid]))

(defn- line-base [{:keys [base-line]} {line-min :min
                                       line-max :max}]
  (let [line-height (- line-max line-min)
        line-base (case base-line
                    :bottom (- line-max)
                    :middle (- (- line-min)
                               (/ line-height 2))
                    :top (- line-min)
                    0)]
    {:line-base line-base
     :line-min (+ line-base line-min)
     :line-max (+ line-base line-max)}))

(defn- pattern-line-with-offset [{pattern-width :width
                                  line-offset :offset
                                  line-mirrored? :mirrored?
                                  spacing :spacing
                                  :as line}
                                 length line-function {:keys [reversed? mirrored?
                                                              num-repetitions] :as line-options}]
  (let [{line-pattern :pattern
         :as pattern-data} (line-function line line-options)
        effective-mirrored? (-> line-mirrored?
                                (util/xor mirrored?)
                                (util/xor reversed?))
        real-spacing (* spacing pattern-width)
        line-pattern (cond-> line-pattern
                       (pos? real-spacing) (cond->
                                             (not effective-mirrored?) (conj "h" real-spacing)
                                             effective-mirrored? (->>
                                                                   (concat ["h" real-spacing]))))
        line-pattern (if effective-mirrored?
                       [(-> (into ["M" 0 0]
                                  line-pattern)
                            path/make-path
                            path/parse-path
                            path/reverse
                            (path/scale -1 1)
                            (path/to-svg :relative? true))]
                       line-pattern)
        {:keys [line-base
                line-min
                line-max]} (line-base line pattern-data)
        offset-length (* line-offset pattern-width)
        pattern-width (+ pattern-width
                         real-spacing)
        repetitions (or num-repetitions
                        (-> length
                            (- offset-length)
                            (/ pattern-width)
                            Math/ceil
                            int
                            inc))
        line-start (v/Vector. (min 0 offset-length) line-base)]
    {:line (-> (if (pos? offset-length)
                 [["h" offset-length]]
                 [])
               (concat (repeat repetitions line-pattern))
               flatten)
     :line-min line-min
     :line-max line-max
     :line-start line-start
     :pattern-width pattern-width
     :height (js/Math.abs line-min)}))

(defn- full-line [line length line-function line-options]
  (let [{line-pattern :pattern
         :as pattern-data} (line-function line length line-options)
        {:keys [line-base
                line-min
                line-max]} (line-base line pattern-data)]
    {:line line-pattern
     :line-min line-min
     :line-max line-max
     :line-start (v/Vector. 0 line-base)
     :height (js/Math.abs line-min)}))

(def ^:private lines
  [#'straight/pattern
   #'invected/pattern
   #'engrailed/pattern
   #'embattled/pattern
   #'embattled-grady/pattern
   #'embattled-in-crosses/pattern
   #'potenty/pattern
   #'indented/pattern
   #'dancetty/pattern
   #'wavy/pattern
   #'nebuly/pattern
   #'dovetailed/pattern
   #'raguly/pattern
   #'thorny/pattern
   #'urdy/pattern
   #'fir-tree-topped/pattern
   #'fir-twigged/pattern
   #'wolf-toothed/pattern
   #'rayonny-flaming/pattern
   #'rayonny-spiked/pattern
   #'angled/pattern
   #'bevilled/pattern
   #'enarched/pattern])

(defn- get-line-identifier [pattern]
  (-> pattern meta :ns name (s/split ".") last keyword))

(def kinds-pattern-map
  (into {}
        (map (fn [pattern]
               [(get-line-identifier pattern) @pattern]))
        lines))

(def ^:private choices
  (map (fn [pattern]
         [(:display-name @pattern) (get-line-identifier pattern)])
       lines))

(def line-map
  (options/choices->map choices))

(def ^:private type-option
  {:type :option.type/choice
   :choices choices
   :default :straight
   :ui/label :string.option/type
   :ui/element :ui.element/line-type-select})

(def ^:private base-line-choices
  [[:string.option.alignment-choice/bottom :bottom]
   [:string.option.alignment-choice/middle :middle]
   [:string.option.alignment-choice/top :top]])

(def base-line-map
  (options/choices->map base-line-choices))

(def ^:private corner-damping-mode-choices
  [[:string.option.damping-mode-choice/clamp-to-zero :clamp-to-zero]
   [:string.option.damping-mode-choice/linear-damping :linear-damping]
   [:string.option.damping-mode-choice/square-root-damping :square-root-damping]])

(def corner-damping-mode-map
  (options/choices->map corner-damping-mode-choices))

(def ^:private size-reference-choices
  [[:string.option.size-reference-choice/line-length :line-length]
   [:string.option.size-reference-choice/field-width :field-width]
   [:string.option.size-reference-choice/field-height :field-height]])

(def size-reference-map
  (options/choices->map size-reference-choices))

(def ^:private default-options
  {:eccentricity {:type :option.type/range
                  :min 0
                  :max 1
                  :default 0.5
                  :ui/label :string.option/eccentricity
                  :ui/step 0.01}
   :height {:type :option.type/range
            :min 0
            :max 3
            :default 1
            :ui/label :string.option/height
            :ui/step 0.01}
   :size-reference {:type :option.type/choice
                    :choices size-reference-choices
                    :default :line-length
                    :ui/label :string.option/size-reference
                    :ui/tooltip :string.tooltip/size-reference}
   :width {:type :option.type/range
           :min 2
           :max 100
           :default 10
           :ui/label :string.option/width
           :ui/step 0.01}
   :offset {:type :option.type/range
            :min -1
            :max 3
            :default 0
            :ui/label :string.option/offset
            :ui/step 0.01}
   :spacing {:type :option.type/range
             :min 0
             :max 5
             :default 0
             :ui/label :string.option/spacing
             :ui/step 0.01}
   :base-line {:type :option.type/choice
               :choices base-line-choices
               :default :middle
               :ui/label :string.option/base-line
               :ui/element :ui.element/radio-select}
   :corner-damping-radius {:type :option.type/range
                           :min 0
                           :max 50
                           :default 0
                           :ui/label :string.option/damping-radius
                           :ui/tooltip :string.tooltip/damping-radius
                           :ui/step 0.01}
   :corner-damping-mode {:type :option.type/choice
                         :choices corner-damping-mode-choices
                         :default :clamp-to-zero
                         :ui/label :string.option/damping-mode
                         :ui/tooltip :string.tooltip/damping-mode
                         :ui/step 0.01}
   :mirrored? {:type :option.type/boolean
               :default false
               :ui/label :string.option/mirrored?}
   :flipped? {:type :option.type/boolean
              :default false
              :ui/label :string.option/flipped?}})

(defn options [{:keys [path] :as context} & {:keys [fimbriation?
                                                    inherited-options
                                                    corner-damping?]
                                             :or {fimbriation? true}}]
  (let [kind (last path)
        inherited (when inherited-options
                    (let [line-context (-> context c/-- (c/++ :line))]
                      (options/sanitize (interface/get-raw-data line-context)
                                        inherited-options)))
        effective-type-option (cond-> type-option
                                inherited (assoc :default (:type inherited)))]
    (when-let [type (options/get-value (interface/get-raw-data (c/++ context :type)) effective-type-option)]
      (-> (case type
            :straight {}
            :invected (options/pick default-options
                                    [[:eccentricity]
                                     [:height]
                                     [:size-reference]
                                     [:width]
                                     [:offset]
                                     [:flipped?]
                                     [:base-line]])
            :engrailed (options/pick default-options
                                     [[:eccentricity]
                                      [:height]
                                      [:size-reference]
                                      [:width]
                                      [:offset]
                                      [:flipped?]
                                      [:base-line]])
            :indented (options/pick default-options
                                    [[:height]
                                     [:size-reference]
                                     [:width]
                                     [:offset]
                                     [:flipped?]
                                     [:base-line]]
                                    {[:width :default] 20})
            :embattled (options/pick default-options
                                     [[:height]
                                      [:size-reference]
                                      [:width]
                                      [:spacing]
                                      [:offset]
                                      [:flipped?]
                                      [:base-line]])
            :embattled-grady (options/pick default-options
                                           [[:height]
                                            [:size-reference]
                                            [:width]
                                            [:spacing]
                                            [:offset]
                                            [:flipped?]
                                            [:base-line]])
            :embattled-in-crosses (options/pick default-options
                                                [[:eccentricity]
                                                 [:height]
                                                 [:size-reference]
                                                 [:width]
                                                 [:spacing]
                                                 [:offset]
                                                 [:flipped?]
                                                 [:base-line]])
            :potenty (options/pick default-options
                                   [[:eccentricity]
                                    [:height]
                                    [:size-reference]
                                    [:width]
                                    [:spacing]
                                    [:offset]
                                    [:flipped?]
                                    [:base-line]])
            :dovetailed (options/pick default-options
                                      [[:eccentricity]
                                       [:height]
                                       [:size-reference]
                                       [:width]
                                       [:spacing]
                                       [:offset]
                                       [:flipped?]
                                       [:base-line]])
            :raguly (options/pick default-options
                                  [[:eccentricity]
                                   [:height]
                                   [:size-reference]
                                   [:width]
                                   [:spacing]
                                   [:offset]
                                   [:mirrored?]
                                   [:flipped?]
                                   [:base-line]])
            :thorny (options/pick default-options
                                  [[:eccentricity]
                                   [:height]
                                   [:size-reference]
                                   [:width]
                                   [:spacing]
                                   [:offset]
                                   [:mirrored?]
                                   [:flipped?]
                                   [:base-line]])
            :dancetty (options/pick default-options
                                    [[:height]
                                     [:size-reference]
                                     [:width]
                                     [:offset]
                                     [:flipped?]
                                     [:base-line]]
                                    {[:width :default] 20})
            :wavy (options/pick default-options
                                [[:eccentricity]
                                 [:height]
                                 [:size-reference]
                                 [:width]
                                 [:offset]
                                 [:mirrored?]
                                 [:flipped?]
                                 [:base-line]]
                                {[:width :default] 20})
            :urdy (options/pick default-options
                                [[:eccentricity]
                                 [:height]
                                 [:size-reference]
                                 [:width]
                                 [:offset]
                                 [:flipped?]
                                 [:base-line]])
            :fir-twigged (options/pick default-options
                                       [[:height]
                                        [:size-reference]
                                        [:width]
                                        [:offset]
                                        [:flipped?]
                                        [:base-line]])
            :fir-tree-topped (options/pick default-options
                                           [[:eccentricity]
                                            [:height]
                                            [:size-reference]
                                            [:width]
                                            [:offset]
                                            [:flipped?]
                                            [:base-line]])
            :wolf-toothed (options/pick default-options
                                        [[:eccentricity]
                                         [:height]
                                         [:size-reference]
                                         [:width]
                                         [:spacing]
                                         [:offset]
                                         [:mirrored?]
                                         [:flipped?]
                                         [:base-line]]
                                        {[:eccentricity :default] 0.5})
            :angled (options/pick default-options
                                  [[:eccentricity]
                                   [:size-reference]
                                   [:width]
                                   [:flipped?]
                                   [:base-line]])
            :bevilled (options/pick default-options
                                    [[:eccentricity]
                                     [:height]
                                     [:size-reference]
                                     [:width]
                                     [:flipped?]
                                     [:base-line]]
                                    {[:width :default] 15})
            :enarched (options/pick default-options
                                    [[:eccentricity]
                                     [:height]
                                     [:size-reference]
                                     [:width]
                                     [:flipped?]
                                     [:base-line]]
                                    {[:width :min] 1
                                     [:width :max] 100
                                     [:width :default] 50
                                     [:height :min] 0
                                     [:height :max] 1
                                     [:height :default] 0.5})
            (options/pick default-options
                          [[:eccentricity]
                           [:height]
                           [:size-reference]
                           [:width]
                           [:offset]
                           [:mirrored?]
                           [:flipped?]
                           [:base-line]]))
          (assoc :type type-option)
          (options/populate-inheritance inherited)
          (cond->
            fimbriation? (assoc :fimbriation (fimbriation/options (c/++ context :fimbriation)
                                                                  :inherited-options (:fimbriation inherited-options)))
            corner-damping? (merge (options/pick default-options
                                                 [[:corner-damping-radius]
                                                  [:corner-damping-mode]]
                                                 {})))
          (assoc :ui/label (case kind
                             :opposite-line :string.entity/opposite-line
                             :extra-line :string.entity/extra-line
                             :string.entity/line)
                 :ui/element :ui.element/line)))))

(defn- get-line-data [{:keys [type]
                       :or {type :straight}
                       :as line}
                      length
                      line-options]
  (let [pattern-data (get kinds-pattern-map type)
        line-function (:function pattern-data)
        line (cond-> line
               (= type :straight) (assoc :width length
                                         :offset 0))
        line-data-fn (if (:full? pattern-data)
                       full-line
                       pattern-line-with-offset)]
    (line-data-fn line length line-function line-options)))

(defn- create-raw [context line length
                   & {:keys [angle flipped? seed reversed?] :as line-options}]
  (let [line (update line :width #(max % 1))
        base-end (v/Vector. length 0)
        line-data (get-line-data line length line-options)
        line-path (path/make-path (into ["M" 0 0] (:line line-data)))
        reversed-path (-> line-path
                          path/parse-path
                          path/reverse)
        line-reversed-start (path/get-start-pos reversed-path)
        line-reversed (path/to-svg reversed-path :from-zero? true)
        line-start (:line-start line-data)
        line-end (-> line-start
                     (v/add line-reversed-start)
                     (v/sub base-end))
        line-path (if reversed?
                    (-> line-reversed
                        path/parse-path
                        (path/scale -1 1)
                        path/to-svg)
                    line-path)
        [line-start line-end] (if reversed?
                                [(v/dot line-end (v/Vector. -1 1))
                                 (v/dot line-start (v/Vector. -1 1))]
                                [line-start line-end])
        line-flipped? (:flipped? line)
        effective-flipped? (util/xor flipped? line-flipped?)
        [line-start line-end] (if effective-flipped?
                                [(v/dot line-start (v/Vector. 1 -1))
                                 (v/dot line-end (v/Vector. 1 -1))]
                                [line-start line-end])
        squiggly? (interface/render-option :squiggly? context)]
    (assoc line-data
           :line (-> line-path
                     (cond->
                       squiggly? (squiggly/squiggly-path :seed seed))
                     path/parse-path
                     (cond->
                       effective-flipped? (path/scale 1 -1))
                     (path/rotate angle)
                     path/to-svg)
           :line-start (when line-start (v/rotate line-start angle))
           :line-end (when line-end (v/rotate (v/add base-end line-end) angle))
           :up (v/rotate (v/Vector. 0 -50) angle)
           :down (v/rotate (v/Vector. 0 50) angle))))

(defn- create [context line from to & {:keys [reversed?] :as line-options}]
  (let [[from to] (if reversed?
                    [to from]
                    [from to])
        direction (v/sub to from)
        length (v/abs direction)
        angle (v/angle-to-point from to)]
    (create-raw context line length
                (merge {:angle angle}
                       line-options))))

(defn- mask-intersection-points [start line-segments direction]
  (->> line-segments
       (map (fn [{:keys [line-end up down]}]
              (let [dv (case direction
                         :up up
                         down)]
                {:start-offset dv
                 :end-offset (v/add line-end dv)})))

       (reduce (fn [current {:keys [start-offset end-offset]}]
                 (if (empty? current)
                   [{:start (v/add start start-offset)
                     :end (v/add start end-offset)}]
                   (let [{previous-end :end} (last current)]
                     (conj current {:start (v/add previous-end start-offset)
                                    :end (v/add previous-end end-offset)})))) [])
       (partition 2 1)
       (map (fn [[{start1 :start end1 :end}
                  {start2 :start end2 :end}]]
              (v/line-intersection start1 end1 start2 end2)))))

(defn line-start [{:keys [adjusted-to adjusted-from line-start
                          reversed?]} & {:keys [reverse?]}]
  (cond-> (if (or (and (not reversed?) reverse?)
                  (and reversed? (not reverse?)))
            adjusted-to
            adjusted-from)
    (not reverse?) (v/add line-start)))

(defn line-end [{:keys [adjusted-to adjusted-from reversed?]} & {:keys [reverse?]}]
  (if (or (and (not reversed?) reverse?)
          (and reversed? (not reverse?)))
    adjusted-from
    adjusted-to))

(defn render [line-segments outline? context]
  (let [first-segment (first line-segments)
        last-segment (last line-segments)
        fimbriation (-> first-segment :line-data :fimbriation)
        line-from (line-start first-segment)
        base-line-start (v/sub line-from (:line-start first-segment))
        base-path (into ["M" line-from]
                        (map (fn [{line-path-snippet :line}]
                               (path/stitch line-path-snippet)))
                        line-segments)
        line-path (path/make-path base-path)
        {:keys [mode
                alignment
                thickness-1
                thickness-2
                tincture-1
                tincture-2
                corner]} fimbriation
        [thickness-1 thickness-2
         tincture-1 tincture-2] (if (and (= alignment :inside)
                                         (= mode :double))
                                  [thickness-2 thickness-1
                                   tincture-2 tincture-1]
                                  [thickness-1 thickness-2
                                   tincture-1 tincture-2])
        mask-shape-top (when (#{:even :outside} alignment)
                         (let [mask-points (into [(v/add line-from (:up first-segment))]
                                                 (mask-intersection-points base-line-start line-segments :up))]
                           (path/make-path [base-path
                                            "l" (:up last-segment)
                                            (map (fn [mask-point]
                                                   ["L" mask-point])
                                                 (reverse mask-points))
                                            "z"])))
        mask-shape-bottom (when (#{:even :inside} alignment)
                            (let [mask-points (into [(v/add line-from (:down first-segment))]
                                                    (mask-intersection-points base-line-start line-segments :down))]
                              (path/make-path [base-path
                                               "l" (:down last-segment)
                                               (map (fn [mask-point]
                                                      ["L" mask-point])
                                                    (reverse mask-points))
                                               "z"])))
        combined-thickness (+ thickness-1 thickness-2)
        mask-id-top (when mask-shape-top
                      (uid/generate "mask-line-top"))
        mask-id-bottom (when mask-shape-bottom
                         (uid/generate "mask-line-bottom"))]
    [:<>
     (when (#{:single :double} mode)
       [:<>
        (when (or mask-shape-top mask-shape-bottom)
          [:defs
           (when mask-shape-top
             [:mask {:id mask-id-top}
              [:path {:d mask-shape-top
                      :fill "#ffffff"}]])
           (when mask-shape-bottom
             [:mask {:id mask-id-bottom}
              [:path {:d mask-shape-bottom
                      :fill "#ffffff"}]])])
        (if (= alignment :even)
          [:<>
           (if (= mode :single)
             [fimbriation/render line-path nil (/ thickness-1 2)
              (tincture/pick tincture-1 context)
              outline? corner context]
             (cond
               (> thickness-1 thickness-2) [:<>
                                            [fimbriation/render line-path nil (/ combined-thickness 2)
                                             (tincture/pick tincture-2 context)
                                             outline? corner context]
                                            [fimbriation/render line-path nil (-> combined-thickness
                                                                                  (/ 2)
                                                                                  (- thickness-2))
                                             (tincture/pick tincture-1 context)
                                             outline? corner context]
                                            [fimbriation/render line-path mask-id-bottom
                                             (cond-> (/ combined-thickness 2)
                                               outline? (- (/ outline/stroke-width 2)))
                                             (tincture/pick tincture-1 context)
                                             false corner context]]
               (= thickness-1 thickness-2) [:<>
                                            [fimbriation/render line-path mask-id-top (/ combined-thickness 2)
                                             (tincture/pick tincture-2 context)
                                             outline? corner context]
                                            [fimbriation/render line-path mask-id-bottom (/ combined-thickness 2)
                                             (tincture/pick tincture-1 context)
                                             outline? corner context]
                                            (when outline?
                                              [fimbriation/render line-path mask-id-bottom 0
                                               nil
                                               outline? corner context])]
               (< thickness-1 thickness-2) [:<>
                                            [fimbriation/render line-path nil (/ combined-thickness 2)
                                             (tincture/pick tincture-1 context)
                                             outline? corner context]
                                            [fimbriation/render line-path nil (-> combined-thickness
                                                                                  (/ 2)
                                                                                  (- thickness-1))
                                             (tincture/pick tincture-2 context)
                                             outline? corner context]
                                            [fimbriation/render line-path mask-id-top
                                             (cond-> (/ combined-thickness 2)
                                               outline? (- (/ outline/stroke-width 2)))
                                             (tincture/pick tincture-2 context)
                                             false corner context]]))]
          [:g {:mask (case alignment
                       :outside (str "url(#" mask-id-top ")")
                       :inside (str "url(#" mask-id-bottom ")")
                       nil)}
           (when (#{:double} mode)
             [fimbriation/render line-path nil combined-thickness
              (tincture/pick tincture-2 context)
              outline? corner context])
           (when (#{:single :double} mode)
             [fimbriation/render line-path nil thickness-1
              (tincture/pick tincture-1 context)
              outline? corner context])])])
     (when (and outline?
                (not (and (= alignment :even)
                          (#{:single :double} mode))))
       [:g (outline/style context)
        [:path {:d line-path}]])]))

(def ^:private add-normals
  (memoize
   (fn add-normals [points]
     (->> (concat [(last points)]
                  points
                  [(first points)])
          (partition 3 1)
          (mapv (fn [[p1 p2 p3]]
               ;; this adds the normal to the LEFT in vector direction p1 -> p3,
               ;; which for a clockwise closed path means the normals are pointing
               ;; outwards
                  (assoc p2 :normal (-> p3
                                        (v/sub p1)
                                        v/orthogonal
                                        v/normal))))))))

(defn- get-point-on-curve [points x-steps x]
  (let [index (-> x
                  (/ x-steps)
                  Math/floor
                  (mod (count points)))
        next-index (-> index
                       inc
                       (mod (count points)))
        p1 (get points index)
        p2 (get points next-index)
        t (mod x x-steps)]
    (-> (v/mul p1 t)
        (v/add (v/mul p2 (- 1 t)))
        (assoc :normal (-> (v/mul (:normal p1) t)
                           (v/add (v/mul (:normal p2) (- 1 t)))
                           v/normal)))))

(defn- dist-to-corner [x {corner-x :x} full-length]
  (min (-> x (- corner-x) Math/abs)
       (-> x (+ full-length) (- corner-x) Math/abs)
       (-> x (- corner-x) (- full-length) Math/abs)))

(defn- process-y-value [x y full-length corners damping-radius damping-mode]
  (let [process-fn (case damping-mode
                     :clamp-to-zero (constantly 0)
                     :linear-damping (fn [y dist]
                                       (-> dist
                                           (/ damping-radius)
                                           (* y)))
                     :square-root-damping (fn [y dist]
                                            (-> dist
                                                (/ damping-radius)
                                                Math/sqrt
                                                (* y))))]
    (if (zero? damping-radius)
      y
      (if-let [dist (->> corners
                         (keep (fn [corner]
                                 (let [dist (dist-to-corner x corner full-length)]
                                   (when (<= dist damping-radius)
                                     dist))))
                         sort
                         first)]
        (process-fn y dist)
        y))))

(def modify-path
  (memoize
   (fn modify-path [path {:keys [type
                                 width
                                 corner-damping-radius
                                 corner-damping-mode]
                          :as line} environment & {:keys [outer-shape?]}]
     (let [pattern-data (get kinds-pattern-map type)
           guiding-path (cond-> path
                          (not (path/clockwise? path)) (->
                                                         path/parse-path
                                                         path/reverse
                                                         path/to-svg))
           full-length (-> guiding-path
                           path/parse-path
                           path/length)
           repetitions (-> (/ full-length width)
                           Math/floor
                           inc)
           pattern-width (/ full-length repetitions)
           precision 0.05
           line-function (:function pattern-data)
           {line-data :line
            line-start :line-start
            real-pattern-width :pattern-width} (pattern-line-with-offset
                                                (assoc line
                                                       :width pattern-width
                                                       :offset 0)
                                                pattern-width
                                                line-function
                                                {:num-repetitions 1})
           offset (-> line
                      :offset
                      (or 0)
                      (* pattern-width))
           fess (-> environment :points :fess)
           top (-> environment :points :top)
           intersection (v/last-intersection-with-shape fess top guiding-path)
           start-offset (-> intersection
                            :t2
                            (* full-length)
                            (+ offset))
           path-points (-> guiding-path
                           (path/sample-path :precision precision
                                             :start-offset start-offset)
                           add-normals)
           sample-total (count path-points)
           path-x-steps (/ full-length sample-total)
           line-pattern-path (-> line-data
                                 path/make-path
                                 (->> (str "M0,0")))
           line-pattern-parsed-path (path/parse-path line-pattern-path)
           sample-per-pattern (-> line-pattern-parsed-path
                                  path/length
                                  (/ precision)
                                  Math/floor)
           line-pattern-points (path/points line-pattern-parsed-path sample-per-pattern)
           corners (mapv (fn [{:keys [index]
                               :as corner}]
                           (assoc corner :x (* index path-x-steps)))
                         (path/find-corners path-points precision 3))]
       (-> (for [pattern-i (range repetitions)
                 pattern-point line-pattern-points]
             (let [real-point (-> (v/add pattern-point line-start)
                                  (v/mul pattern-width)
                                  (v/div real-pattern-width))
                   x-in-pattern (:x real-point)
                   x-on-path (-> pattern-i
                                 (* full-length)
                                 (/ repetitions)
                                 (+ x-in-pattern)
                                 (+ offset))
                   point-on-curve (get-point-on-curve path-points path-x-steps x-on-path)
                   y-dir (:normal point-on-curve)
                   y-value (process-y-value x-on-path (:y real-point) full-length corners
                                            corner-damping-radius corner-damping-mode)]
            ;; the y-value will usually be negative, but the normals
            ;; will also point outwards from the shape, so this will
            ;; build a path on the inside of the shape
            ;; however, if outer-shape? is true, then flip the normal
               (-> y-dir
                   (cond->
                     outer-shape? (v/mul -1))
                   (v/mul y-value)
                   (v/add point-on-curve))))
           (->> (map-indexed (fn [idx p]
                               [(if (zero? idx)
                                  "M" "L") p])))
           path/make-path
           (str "z"))))))

(defn- calculate-effective-height [{:keys [line-length
                                           fimbriation] :as line}]
  (let [fimbriation-alignment (:alignment fimbriation)
        fimbriation-thickness (+ (or (:thickness-1 fimbriation) 0)
                                 (or (:thickness-2 fimbriation) 0))
        effective-line-height (or (:height (get-line-data line line-length {})) 0)]
    (assoc line
           :effective-height (+ effective-line-height
                                (case fimbriation-alignment
                                  :even (/ fimbriation-thickness 2)
                                  :outside fimbriation-thickness
                                  :inside 0
                                  0)))))

(defn resolve-percentages [line line-length field-width field-height fimbriation-percentage-base]
  (let [line-percentage-base (case (:size-reference line)
                               :field-width field-width
                               :field-height field-height
                               line-length)]
    (-> line
        (update :width (partial math/percent-of line-percentage-base))
        (update-in [:fimbriation :thickness-1] (partial math/percent-of fimbriation-percentage-base))
        (update-in [:fimbriation :thickness-2] (partial math/percent-of fimbriation-percentage-base))
        (assoc :line-length line-length)
        calculate-effective-height)))

(defn- extend [start extra bounding-box]
  (let [next (v/add start extra)]
    (if (bb/surrounds? bounding-box next)
      (recur next extra bounding-box)
      next)))

(defn create-with-extension [context
                             {:keys [line-length]
                              :as line} from to bounding-box & {:keys [extend-from?
                                                                       extend-to?
                                                                       reversed?]
                                                                :or {extend-from? true
                                                                     extend-to? true}
                                                                :as line-options}]
  (let [dir (v/normal (v/sub to from))
        dir (if (= dir v/zero)
              (v/Vector. 1 0)
              dir)
        width (max (or (:width line) 10) 1)
        extra (v/mul dir width)
        from (or from (v/Vector. -10 -50))
        to (or to (v/Vector. 110 50))
        bounding-box (bb/dilate bounding-box 20)
        extended-from (cond-> from
                        extend-from? (extend (v/mul extra -1) bounding-box))
        extended-to (cond-> to
                      extend-to? (extend extra bounding-box))
        extended-to (if (and line-length
                             (< (v/abs (v/sub from extended-to)) line-length))
                      (v/add from (v/mul dir line-length))
                      extended-to)
        real-start (v/abs (v/sub from extended-from))
        real-end (or (+ real-start line-length)
                     (v/abs (v/sub to extended-from)))]
    (assoc (create context line extended-from extended-to (assoc line-options
                                                                 :real-start real-start
                                                                 :real-end real-end))
           :line-data line
           :reversed? reversed?
           :adjusted-from extended-from
           :adjusted-to extended-to)))

(defn reversed-path [line-path]
  (-> line-path
      path/parse-path
      path/reverse
      (path/to-svg :relative? true)))
