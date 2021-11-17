(ns heraldry.coat-of-arms.line.core
  (:require
   ["svgpath" :as svgpath]
   [clojure.string :as s]
   [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
   [heraldry.coat-of-arms.line.type.angled :as angled]
   [heraldry.coat-of-arms.line.type.bevilled :as bevilled]
   [heraldry.coat-of-arms.line.type.dancetty :as dancetty]
   [heraldry.coat-of-arms.line.type.dovetailed :as dovetailed]
   [heraldry.coat-of-arms.line.type.embattled :as embattled]
   [heraldry.coat-of-arms.line.type.embattled-grady :as embattled-grady]
   [heraldry.coat-of-arms.line.type.embattled-in-crosses :as embattled-in-crosses]
   [heraldry.coat-of-arms.line.type.enarched :as enarched]
   [heraldry.coat-of-arms.line.type.engrailed :as engrailed]
   [heraldry.coat-of-arms.line.type.fir-tree-topped :as fir-tree-topped]
   [heraldry.coat-of-arms.line.type.fir-twigged :as fir-twigged]
   [heraldry.coat-of-arms.line.type.indented :as indented]
   [heraldry.coat-of-arms.line.type.invected :as invected]
   [heraldry.coat-of-arms.line.type.nebuly :as nebuly]
   [heraldry.coat-of-arms.line.type.potenty :as potenty]
   [heraldry.coat-of-arms.line.type.raguly :as raguly]
   [heraldry.coat-of-arms.line.type.rayonny-flaming :as rayonny-flaming]
   [heraldry.coat-of-arms.line.type.rayonny-spiked :as rayonny-spiked]
   [heraldry.coat-of-arms.line.type.straight :as straight]
   [heraldry.coat-of-arms.line.type.thorny :as thorny]
   [heraldry.coat-of-arms.line.type.urdy :as urdy]
   [heraldry.coat-of-arms.line.type.wavy :as wavy]
   [heraldry.coat-of-arms.line.type.wolf-toothed :as wolf-toothed]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.svg.squiggly :as squiggly]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(defn line-base [{:keys [base-line]} {line-min :min
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

(defn pattern-line-with-offset [{pattern-width :width
                                 line-offset :offset
                                 line-mirrored? :mirrored?
                                 spacing :spacing
                                 :as line}
                                length line-function {:keys [reversed? mirrored?] :as line-options}]
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
                       [(-> line-pattern
                            (->> (into ["M" 0 0]))
                            path/make-path
                            path/reverse-path
                            :path
                            svgpath
                            (.scale -1 1)
                            .toString
                            (s/replace "M0 0" ""))]
                       line-pattern)
        {:keys [line-base
                line-min
                line-max]} (line-base line pattern-data)
        offset-length (* line-offset pattern-width)
        pattern-width (+ pattern-width
                         real-spacing)
        repetitions (-> length
                        (- offset-length)
                        (/ pattern-width)
                        Math/ceil
                        int
                        inc)
        line-start (v/v (min 0 offset-length) line-base)]
    {:line (-> []
               (cond->
                 (pos? offset-length) (into [["h" offset-length]]))
               (into (repeat repetitions line-pattern))
               (->> (apply merge))
               vec)
     :line-min line-min
     :line-max line-max
     :line-start line-start}))

(defn full-line [line length line-function line-options]
  (let [{line-pattern :pattern
         :as pattern-data} (line-function line length line-options)
        {:keys [line-base
                line-min
                line-max]} (line-base line pattern-data)]
    {:line line-pattern
     :line-min line-min
     :line-max line-max
     :line-start (v/v 0 line-base)}))

(def lines
  [#'straight/full
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
   #'angled/full
   #'bevilled/full
   #'enarched/full])

(def kinds-function-map
  (->> lines
       (map (fn [function]
              [(-> function meta :value) function]))
       (into {})))

(def choices
  (->> lines
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :value)]))))

(def line-map
  (util/choices->map choices))

(def type-option
  {:type :choice
   :choices choices
   :default :straight
   :ui {:label strings/type
        :form-type :line-type-select}})

(def default-options
  {:eccentricity {:type :range
                  :min 0
                  :max 1
                  :default 0.5
                  :ui {:label strings/eccentricity
                       :step 0.01}}
   :height {:type :range
            :min 0
            :max 3
            :default 1
            :ui {:label strings/height
                 :step 0.01}}
   :width {:type :range
           :min 2
           :max 100
           :default 10
           :ui {:label strings/width
                :step 0.01}}
   :offset {:type :range
            :min -1
            :max 3
            :default 0
            :ui {:label strings/offset
                 :step 0.01}}
   :spacing {:type :range
             :min 0
             :max 5
             :default 0
             :ui {:label strings/spacing
                  :step 0.01}}
   :base-line {:type :choice
               :choices [[strings/bottom :bottom]
                         [strings/middle :middle]
                         [strings/top :top]]
               :default :middle
               :ui {:label strings/base-line
                    :form-type :radio-select}}
   :mirrored? {:type :boolean
               :default false
               :ui {:label strings/mirrored}}
   :flipped? {:type :boolean
              :default false
              :ui {:label strings/flipped}}})

(defn options-subscriptions [context & {:keys [fimbriation?]
                                        :or {fimbriation? true}}]
  (let [line-key (-> context :path last)]
    (-> #{}
        (conj [line-key :type])
        (cond->
          fimbriation? (conj [line-key :fimbriation :mode])
          (#{:opposite-key
             :extra-key} line-key) (into (-> context
                                             c/--
                                             (c/++ :line)
                                             options-subscriptions))))))

(defn options [{:keys [path] :as context} & {:keys [fimbriation?]
                                             :or {fimbriation? true}}]
  (let [kind (last path)
        inherited (when (#{:opposite-line
                           :extra-line} kind)
                    (let [line-context (-> context c/-- (c/++ :line))]
                      (options/sanitize (interface/get-raw-data line-context)
                                        (options line-context :fimbriation? fimbriation?))))
        effective-type-option (cond-> type-option
                                inherited (assoc :default (:type inherited)))]
    (when-let [type (options/get-value (interface/get-raw-data (c/++ context :type)) effective-type-option)]
      (-> (case type
            :straight {}
            :invected (options/pick default-options
                                    [[:eccentricity]
                                     [:height]
                                     [:width]
                                     [:offset]
                                     [:flipped?]
                                     [:base-line]])
            :engrailed (options/pick default-options
                                     [[:eccentricity]
                                      [:height]
                                      [:width]
                                      [:offset]
                                      [:flipped?]
                                      [:base-line]])
            :indented (options/pick default-options
                                    [[:height]
                                     [:width]
                                     [:offset]
                                     [:flipped?]
                                     [:base-line]])
            :embattled (options/pick default-options
                                     [[:height]
                                      [:width]
                                      [:spacing]
                                      [:offset]
                                      [:flipped?]
                                      [:base-line]])
            :embattled-grady (options/pick default-options
                                           [[:height]
                                            [:width]
                                            [:spacing]
                                            [:offset]
                                            [:flipped?]
                                            [:base-line]])
            :embattled-in-crosses (options/pick default-options
                                                [[:eccentricity]
                                                 [:height]
                                                 [:width]
                                                 [:spacing]
                                                 [:offset]
                                                 [:flipped?]
                                                 [:base-line]])
            :potenty (options/pick default-options
                                   [[:eccentricity]
                                    [:height]
                                    [:width]
                                    [:spacing]
                                    [:offset]
                                    [:flipped?]
                                    [:base-line]])
            :dovetailed (options/pick default-options
                                      [[:eccentricity]
                                       [:height]
                                       [:width]
                                       [:spacing]
                                       [:offset]
                                       [:flipped?]
                                       [:base-line]])
            :raguly (options/pick default-options
                                  [[:eccentricity]
                                   [:height]
                                   [:width]
                                   [:spacing]
                                   [:offset]
                                   [:mirrored?]
                                   [:flipped?]
                                   [:base-line]])
            :thorny (options/pick default-options
                                  [[:eccentricity]
                                   [:height]
                                   [:width]
                                   [:spacing]
                                   [:offset]
                                   [:mirrored?]
                                   [:flipped?]
                                   [:base-line]])
            :dancetty (options/pick default-options
                                    [[:height]
                                     [:width]
                                     [:offset]
                                     [:flipped?]
                                     [:base-line]]
                                    {[:width :default] 20})
            :wavy (options/pick default-options
                                [[:eccentricity]
                                 [:height]
                                 [:width]
                                 [:offset]
                                 [:mirrored?]
                                 [:flipped?]
                                 [:base-line]]
                                {[:width :default] 20})
            :urdy (options/pick default-options
                                [[:eccentricity]
                                 [:height]
                                 [:width]
                                 [:offset]
                                 [:flipped?]
                                 [:base-line]])
            :fir-twigged (options/pick default-options
                                       [[:height]
                                        [:width]
                                        [:offset]
                                        [:flipped?]
                                        [:base-line]])
            :fir-tree-topped (options/pick default-options
                                           [[:eccentricity]
                                            [:height]
                                            [:width]
                                            [:offset]
                                            [:flipped?]
                                            [:base-line]])
            :wolf-toothed (options/pick default-options
                                        [[:eccentricity]
                                         [:height]
                                         [:width]
                                         [:spacing]
                                         [:offset]
                                         [:mirrored?]
                                         [:flipped?]
                                         [:base-line]]
                                        {[:eccentricity :default] 0.5})
            :angled (options/pick default-options
                                  [[:eccentricity]
                                   [:width]
                                   [:flipped?]
                                   [:base-line]])
            :bevilled (options/pick default-options
                                    [[:eccentricity]
                                     [:height]
                                     [:width]
                                     [:flipped?]
                                     [:base-line]]
                                    {[:width :default] 15})
            :enarched (options/pick default-options
                                    [[:eccentricity]
                                     [:height]
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
                           [:width]
                           [:offset]
                           [:mirrored?]
                           [:flipped?]
                           [:base-line]]))
          (assoc :type type-option)
          (options/populate-inheritance inherited)
          (cond->
            fimbriation? (assoc :fimbriation (fimbriation/options (c/++ context :fimbriation))))
          (assoc :ui {:label (case kind
                               :opposite-line strings/opposite-line
                               :extra-line strings/extra-line
                               strings/line)
                      :form-type :line})))))

(defn create-raw [{:keys [type] :or {type :straight} :as line} length
                  & {:keys [angle flipped? context seed reversed?] :as line-options}]
  (let [line-function (get kinds-function-map type)
        line-options-values (cond-> line #_(options/sanitize line (options line))
                              (= type :straight) (-> (assoc :width length)
                                                     (assoc :offset 0)))
        base-end (v/v length 0)
        line-data (if (-> line-function
                          meta
                          :full?)
                    (full-line
                     line-options-values
                     length
                     line-function
                     line-options)
                    (pattern-line-with-offset
                     line-options-values
                     length
                     line-function
                     line-options))
        line-path (-> line-data
                      :line
                      (->> (into ["M" 0 0]))
                      path/make-path)
        {line-reversed-start :start
         line-reversed :path} (-> line-path
                                  path/reverse-path)
        line-start (:line-start line-data)
        line-end (-> line-start
                     (v/add line-reversed-start)
                     (v/sub base-end))
        line-path (if reversed?
                    (-> line-reversed
                        svgpath
                        (.scale -1 1)
                        .toString)
                    line-path)
        [line-start line-end] (if reversed?
                                [(v/dot line-end (v/v -1 1))
                                 (v/dot line-start (v/v -1 1))]
                                [line-start line-end])
        line-flipped? (:flipped? line-options-values)
        effective-flipped? (heraldry.util/xor flipped? line-flipped?)
        [line-start line-end] (if effective-flipped?
                                [(v/dot line-start (v/v 1 -1))
                                 (v/dot line-end (v/v 1 -1))]
                                [line-start line-end])
        squiggly? (interface/render-option :squiggly? context)]
    (-> line-data
        (assoc :line
               (-> line-path
                   (cond->
                     squiggly? (squiggly/squiggly-path :seed seed))
                   svgpath
                   (cond->
                     effective-flipped? (.scale 1 -1))
                   (.rotate angle)
                   .toString))
        (assoc :line-start (when line-start (v/rotate line-start angle)))
        (assoc :line-end (when line-end (v/rotate (v/add base-end line-end) angle)))
        (assoc :up (v/rotate (v/v 0 -50) angle))
        (assoc :down (v/rotate (v/v 0 50) angle)))))

(defn get-intersections-before-and-after [t intersections]
  (let [before (or (-> intersections
                       (->> (filter #(<= (:t1 %) t)))
                       last
                       :t1)
                   0)
        after (or (-> intersections
                      (->> (filter #(> (:t1 %) t)))
                      first
                      :t1)
                  1)]
    [before after]))

(defn find-real-start-and-end [from to {:keys [environment reversed?
                                               real-start real-end]}]
  (if (and real-start real-end)
    [real-start real-end]
    (let [[from to] (if reversed?
                      [to from]
                      [from to])
          direction (v/sub to from)
          length (v/abs direction)
          intersections (v/find-intersections from to environment)
          [start-t end-t] (get-intersections-before-and-after 0.5 intersections)
          [start-t end-t] (if reversed?
                            [(- 1 end-t) (- 1 start-t)]
                            [start-t end-t])
          real-start (* length start-t)
          real-end (* length end-t)]
      [real-start real-end])))

(defn create [line from to & {:keys [reversed?] :as line-options}]
  (let [[from to] (if reversed?
                    [to from]
                    [from to])
        direction (v/sub to from)
        length (v/abs direction)
        [real-start real-end] (find-real-start-and-end from to line-options)
        angle (v/angle-to-point from to)]
    (apply create-raw (into [line length]
                            (mapcat identity (merge
                                              {:real-start real-start
                                               :real-end real-end
                                               :angle angle}
                                              line-options))))))

(defn mask-intersection-points [start line-datas direction]
  (->> line-datas
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

(defn render [line line-datas start outline? context]
  (let [{:keys [fimbriation]} line
        line-start (-> line-datas first :line-start)
        base-path ["M" (v/add start line-start)
                   (for [{line-path-snippet :line} line-datas]
                     (path/stitch line-path-snippet))]
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
                         (let [mask-points (-> [(v/add start line-start (-> line-datas first :up))]
                                               (into (mask-intersection-points start line-datas :up)))]
                           (path/make-path [base-path
                                            "l" (-> line-datas last :up)
                                            (for [mask-point (reverse mask-points)]
                                              ["L" mask-point])
                                            "z"])))
        mask-shape-bottom (when (#{:even :inside} alignment)
                            (let [mask-points (-> [(v/add start line-start (-> line-datas first :down))]
                                                  (into (mask-intersection-points start line-datas :down)))]
                              (path/make-path [base-path
                                               "l" (-> line-datas last :down)
                                               (for [mask-point (reverse mask-points)]
                                                 ["L" mask-point])
                                               "z"])))
        combined-thickness (+ thickness-1 thickness-2)
        mask-id-top (when mask-shape-top
                      (util/id "mask-line-top"))
        mask-id-bottom (when mask-shape-bottom
                         (util/id "mask-line-bottom"))]
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
                                               outline? (- outline/stroke-width))
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
                                               outline? (- outline/stroke-width))
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
