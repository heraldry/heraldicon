(ns heraldry.render
  (:require
   [clojure.string :as s]
   [heraldry.backend.output :as output]
   [heraldry.coat-of-arms.escutcheon :as escutcheon]
   [heraldry.coat-of-arms.field.environment :as environment]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.hatching :as hatching]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.texture :as texture]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.colour :as colour]
   [heraldry.context :as c]
   [heraldry.font :as font]
   [heraldry.interface :as interface]
   [heraldry.math.bounding-box :as bounding-box]
   [heraldry.math.filter :as filter]
   [heraldry.math.svg.path :as path]
   [heraldry.math.svg.squiggly :as squiggly]
   [heraldry.math.vector :as v]
   [heraldry.ribbon :as ribbon]
   [heraldry.svg.metadata :as svg-metadata]
   [heraldry.util :as util]))

(defn coat-of-arms [{:keys
                     [svg-export?
                      metadata-path
                      texture-link] :as context} width]
  (let [mode (interface/render-option :mode context)
        escutcheon (interface/render-option :escutcheon context)
        escutcheon-shadow? (when-not svg-export?
                             (interface/render-option :escutcheon-shadow? context))
        escutcheon-outline? (interface/render-option :escutcheon-outline? context)
        outline? (interface/render-option :outline? context)
        shiny? (interface/render-option :shiny? context)
        squiggly? (interface/render-option :squiggly? context)
        theme (interface/render-option :theme context)
        texture (interface/render-option :texture context)
        texture-displacement? (interface/render-option :texture-displacement? context)
        flag-width (interface/render-option :flag-width context)
        flag-height (interface/render-option :flag-height context)
        flag-swallow-tail (interface/render-option :flag-swallow-tail context)
        flag-tail-point-height (interface/render-option :flag-tail-point-height context)
        flag-tail-tongue (interface/render-option :flag-tail-tongue context)
        shield (escutcheon/field escutcheon flag-width flag-height flag-swallow-tail
                                 flag-tail-point-height flag-tail-tongue)
        environment (-> (environment/transform-to-width shield width)
                        (cond->
                          squiggly? (update-in [:shape :paths]
                                               #(into []
                                                      (map squiggly/squiggly-path)
                                                      %))))
        mask-id (util/id "mask")
        texture-id (util/id "texture")
        shiny-id (util/id "shiny")
        texture-link (or texture-link (texture/full-path texture))]
    {:environment environment
     :result [:g {:filter (when escutcheon-shadow?
                            "url(#shadow)")}
              (when metadata-path
                [svg-metadata/attribution (assoc context :path metadata-path) :arms])
              [:defs
               (when shiny?
                 [:filter {:id shiny-id}
                  [:feDiffuseLighting {:in "SourceGraphic"
                                       :result "light"
                                       :lighting-color "white"}
                   [:fePointLight {:x 75
                                   :y 20
                                   :z 20}]]
                  [:feComposite {:in "SourceGraphic"
                                 :in2 "light"
                                 :operator "arithmetic"
                                 :k1 1
                                 :k2 0
                                 :k3 0
                                 :k4 0}]])
               (when texture-link
                 [:filter {:id texture-id}
                  [:feImage {:href texture-link
                             :x 0
                             :y 0
                             :width 150
                             :height 150
                             :preserveAspectRatio "none"
                             :result "image"}]
                  (when texture-displacement?
                    [:feDisplacementMap {:in "SourceGraphic"
                                         :in2 "image"
                                         :scale (texture/displacement texture)
                                         :xChannelSelector "R"
                                         :yChannelSelector "R"
                                         :result "displaced"}])
                  [:feComposite {:in (if texture-displacement?
                                       "displaced"
                                       "SourceGraphic")
                                 :in2 "image"
                                 :operator "arithmetic"
                                 :k1 1
                                 :k2 0
                                 :k3 0
                                 :k4 0}]])
               (when-not svg-export?
                 filter/shadow)
               [tincture/patterns theme]
               (when (= mode :hatching)
                 hatching/patterns)]
              [:defs
               [(if svg-export?
                  :mask
                  :clipPath)
                {:id mask-id}
                [:path {:d (s/join "" (-> environment :shape :paths))
                        :clip-rule "evenodd"
                        :fill-rule "evenodd"
                        :fill "#fff"
                        :stroke "none"}]]]
              [:g {(if svg-export?
                     :mask
                     :clip-path) (str "url(#" mask-id ")")}
               [:g {:filter (when texture-link (str "url(#" texture-id ")"))}
                [:g {:filter (when shiny?
                               (str "url(#" shiny-id ")"))}
                 [:path {:d (s/join "" (-> environment :shape :paths))
                         :fill-rule "evenodd"
                         :fill "#f0f0f0"}]
                 [field-shared/render (-> context
                                          (c/++ :field)
                                          (assoc :environment environment)
                                          (dissoc :metadata-path))]]]]
              (when (or escutcheon-outline?
                        outline?)
                [:g (outline/style context)
                 [:path {:d (s/join "" (-> environment :shape :paths))}]])]}))

(defn helm [context & {:keys [below-shield?]}]
  [:<>
   (doall
    (for [[idx self-below-shield?] (interface/get-element-indices
                                    (c/++ context :components))]
      ^{:key idx}
      [interface/render-component
       (-> context
           (c/++ :components idx)
           (assoc :auto-resize? false)
           (assoc :self-below-shield? self-below-shield?)
           (assoc :render-pass-below-shield? below-shield?))]))])

(defn helms [context width]
  (let [num-helms (interface/get-list-size (c/++ context :elements))]
    (if (zero? num-helms)
      {:width 0
       :height 0
       :result nil}
      (let [gap-part-fn (fn [n] (+ 2 (* 2 (- n 1))))
            gap-part (gap-part-fn num-helms)
            helm-width (/ (* gap-part width)
                          (+ (* (+ gap-part 1)
                                num-helms)
                             1))
            helm-height (* 2 helm-width)
            total-height helm-height
            gap (/ helm-width gap-part)
            helm-width-with-gap (+ helm-width gap)]
        {:width (- width
                   (* 2 gap))
         :height total-height
         :result-below-shield [:g
                               (doall
                                (for [idx (range num-helms)]
                                  (let [helm-environment (environment/create
                                                          nil
                                                          {:bounding-box [(+ (* idx helm-width-with-gap)
                                                                             gap)
                                                                          (+ (* idx helm-width-with-gap)
                                                                             gap
                                                                             helm-width)
                                                                          (- helm-height)
                                                                          0]})]
                                    ^{:key idx}
                                    [helm (-> context
                                              (c/++ :elements idx)
                                              (assoc :environment helm-environment))
                                     :below-shield? true])))]
         :result-above-shield [:g
                               (doall
                                (for [idx (range num-helms)]
                                  (let [helm-environment (environment/create
                                                          nil
                                                          {:bounding-box [(+ (* idx helm-width-with-gap)
                                                                             gap)
                                                                          (+ (* idx helm-width-with-gap)
                                                                             gap
                                                                             helm-width)
                                                                          (- helm-height)
                                                                          0]})]
                                    ^{:key idx}
                                    [helm (-> context
                                              (c/++ :elements idx)
                                              (assoc :environment helm-environment))])))]}))))

(defn ribbon [{:keys [select-component-fn
                      svg-export?] :as context}
              tincture-foreground
              tincture-background
              tincture-text
              & {:keys [outline-thickness]
                 :or {outline-thickness 1}}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        edge-angle (interface/get-sanitized-data (c/++ context :edge-angle))
        end-split (interface/get-sanitized-data (c/++ context :end-split))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (interface/get-raw-data (c/++ context :points))
        segments (interface/get-raw-data (c/++ context :segments))
        {:keys [curves edge-vectors]} (ribbon/generate-curves points edge-angle)
        num-curves (count curves)
        foreground-colour (tincture/pick tincture-foreground context)
        background-colour (if (= tincture-background :none)
                            (colour/darken foreground-colour)
                            (tincture/pick tincture-background context))
        text-colour (tincture/pick tincture-text context)]
    [:g (when-not svg-export?
          {:on-click (when select-component-fn
                       #(select-component-fn % (c/-- context)))
           :style {:cursor "pointer"}})
     (doall
      (for [[idx partial-curve] (->> curves
                                     (map-indexed vector)
                                     (sort-by (fn [[idx _]]
                                                [(-> segments
                                                     (get idx)
                                                     :z-index
                                                     (or 1000))
                                                 idx])))]
        (let [top-edge (path/curve-to-relative partial-curve)
              [first-edge-vector second-edge-vector] (get edge-vectors idx)
              first-edge-vector (v/mul first-edge-vector thickness)
              second-edge-vector (v/mul second-edge-vector thickness)
              full-path (cond
                          (or (zero? end-split)
                              (< 0 idx (dec num-curves))) (str top-edge
                                                               (path/line-to second-edge-vector)
                                                               (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                               (path/line-to (v/mul first-edge-vector -1)))
                          ;; special case of only one segment with end-split > 0
                          (and (pos? end-split)
                               (zero? idx)
                               (= idx (dec num-curves))) (str top-edge
                                                              (util/combine " "
                                                                            (ribbon/split-end
                                                                             :end
                                                                             partial-curve
                                                                             (/ end-split 2)
                                                                             second-edge-vector))
                                                              (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                              (util/combine " "
                                                                            (ribbon/split-end
                                                                             :start
                                                                             partial-curve
                                                                             (/ end-split 2)
                                                                             first-edge-vector)))
                          (and (pos? end-split)
                               (zero? idx)) (str top-edge
                                                 (path/line-to second-edge-vector)
                                                 (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                 (util/combine " "
                                                               (ribbon/split-end
                                                                :start
                                                                partial-curve
                                                                end-split
                                                                first-edge-vector)))
                          (and (pos? end-split)
                               (= idx (dec num-curves))) (str top-edge
                                                              (util/combine " "
                                                                            (ribbon/split-end
                                                                             :end
                                                                             partial-curve
                                                                             end-split
                                                                             second-edge-vector))
                                                              (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                              (path/line-to (v/mul first-edge-vector -1))))
              segment-context (c/++ context :segments idx)
              segment-type (interface/get-raw-data (c/++ segment-context :type))
              foreground? (#{:heraldry.ribbon.segment/foreground
                             :heraldry.ribbon.segment/foreground-with-text} segment-type)
              text (some-> (interface/get-sanitized-data (c/++ segment-context :text))
                           (s/replace #"[*]" "⬪"))
              text? (and (= segment-type :heraldry.ribbon.segment/foreground-with-text)
                         (some-> text
                                 s/trim
                                 count
                                 pos?))]
          ^{:key idx}
          [:g
           [:path {:d full-path
                   :style (merge (when outline?
                                   (outline/style context))
                                 (when outline?
                                   {:stroke-width outline-thickness})
                                 {:fill (if foreground?
                                          foreground-colour
                                          background-colour)})}]
           (when text?
             (let [path-id (util/id "path")
                   spacing (interface/get-sanitized-data (c/++ segment-context :spacing))
                   offset-x (interface/get-sanitized-data (c/++ segment-context :offset-x))
                   offset-y (interface/get-sanitized-data (c/++ segment-context :offset-y))
                   font (some-> (interface/get-sanitized-data (c/++ segment-context :font))
                                font/css-string)
                   font-scale (interface/get-sanitized-data (c/++ segment-context :font-scale))
                   font-size (* font-scale thickness)
                   spacing (* spacing font-size)
                   text-path-start (v/add (ffirst partial-curve)
                                          (v/mul first-edge-vector (- 0.6 offset-y)))
                   text-path-end (v/add (last (last partial-curve))
                                        (v/mul second-edge-vector (- 0.6 offset-y)))
                   text-path (ribbon/project-path-to
                              partial-curve
                              text-path-start
                              text-path-end)]
               [:text.no-select {:fill text-colour
                                 :text-anchor "middle"
                                 :style {:font-family font
                                         :font-size font-size}}
                [:defs
                 [:path {:id path-id
                         :d (str "M " (v/->str text-path-start) " " text-path)}]]
                [:textPath {:href (str "#" path-id)
                            :alignment-baseline "middle"
                            :method "align"
                            :lengthAdjust "spacing"
                            :letter-spacing spacing
                            :startOffset (str (+ 50 (* offset-x 100)) "%")}
                 text]]))])))]))

(defn motto [{:keys [environment
                     self-below-shield?
                     render-pass-below-shield?]
              :as context}]
  (let [{:keys [width height]} environment
        points (interface/get-raw-data (c/++ context :ribbon :points))]
    (if (and (= (boolean self-below-shield?)
                (boolean render-pass-below-shield?))
             points)
      (let [origin-point (interface/get-sanitized-data (c/++ context :origin :point))
            tincture-foreground (interface/get-sanitized-data (c/++ context :tincture-foreground))
            tincture-background (interface/get-sanitized-data (c/++ context :tincture-background))
            tincture-text (interface/get-sanitized-data (c/++ context :tincture-text))
            offset-x (interface/get-sanitized-data (c/++ context :origin :offset-x))
            offset-y (interface/get-sanitized-data (c/++ context :origin :offset-y))
            size (interface/get-sanitized-data (c/++ context :geometry :size))
            thickness (interface/get-sanitized-data (c/++ context :ribbon :thickness))
            position (-> (-> environment :points (get origin-point))
                         (v/add (v/v ((util/percent-of width) offset-x)
                                     (- ((util/percent-of height) offset-y)))))
            ;; TODO: not ideal, need the thickness here and need to know that the edge-vector (here
            ;; assumed to be (0 thickness) as a max) needs to be added to every point for the correct
            ;; height; could perhaps be a subscription or the ribbon function can provide it?
            ;; but then can't use the ribbon function as reagent component
            [min-x max-x
             min-y max-y] (bounding-box/min-max-x-y (concat points
                                                            (map (partial v/add (v/v 0 thickness)) points)))
            ribbon-width (- max-x min-x)
            ribbon-height (- max-y min-y)
            target-width ((util/percent-of width) size)
            scale (/ target-width ribbon-width)
            outline-thickness (/ outline/stroke-width
                                 2
                                 scale)]
        {:result [:g {:transform (str "translate(" (v/->str position) ")"
                                      "scale(" scale "," scale ")"
                                      "translate(" (v/->str (-> (v/v ribbon-width ribbon-height)
                                                                (v/div 2)
                                                                (v/add (v/v min-x min-y))
                                                                (v/mul -1))) ")")}
                  [ribbon
                   (c/++ context :ribbon)
                   tincture-foreground
                   tincture-background
                   tincture-text
                   :outline-thickness outline-thickness]]
         :bounding-box (let [top-left (-> (v/v min-x min-y)
                                          (v/mul scale)
                                          (v/add position))
                             bottom-right (-> (v/v max-x max-y)
                                              (v/mul scale)
                                              (v/add position))]

                         [(:x top-left) (:x bottom-right)
                          (:y top-left) (:y bottom-right)])})
      {:result nil
       :bounding-box nil})))

(defn ornaments-elements [context & {:keys [below-shield?]}]
  [:<>
   (doall
    (for [[idx self-below-shield?] (interface/get-element-indices context)]
      (let [updated-context (-> context
                                (c/++ idx)
                                (assoc :auto-resize? false)
                                (assoc :self-below-shield? self-below-shield?)
                                (assoc :render-pass-below-shield? below-shield?))
            motto? (interface/motto? updated-context)]
        ^{:key idx}
        [:<>
         (if motto?
           (:result (motto updated-context))
           [interface/render-component updated-context])])))])

(defn ornaments [context coa-bounding-box]
  (let [[bb-min-x bb-max-x bb-min-y bb-max-y] coa-bounding-box
        num-ornaments (count (interface/get-element-indices (c/++ context :elements)))
        width (- bb-max-x bb-min-x)
        height (- bb-max-y bb-min-y)
        ornaments-width (* 3 1.2 width)
        ornaments-height (* 2 1.2 height)
        ornaments-left (-> width
                           (/ 2)
                           (- (/ ornaments-width 2))
                           (+ bb-min-x))
        ornaments-top (-> height
                          (/ 2)
                          (- (/ ornaments-height 2))
                          (+ bb-min-y))
        environment (environment/create
                     nil
                     {:bounding-box coa-bounding-box})
        updated-context (-> context
                            (c/++ :elements)
                            (assoc :environment environment))]
    (if (pos? num-ornaments)
      {:result-below-shield [ornaments-elements updated-context :below-shield? true]
       :result-above-shield [ornaments-elements updated-context :below-shield? false]

       :bounding-box [ornaments-left
                      (+ ornaments-left ornaments-width)
                      ornaments-top
                      (+ ornaments-top ornaments-height)]}
      {:bounding-box [0 0 0 0]})))

(defn transform-bounding-box [[min-x max-x min-y max-y] target-width & {:keys [max-aspect-ratio]}]
  (let [total-width (- max-x min-x)
        total-height (- max-y min-y)
        target-height (-> target-width
                          (/ total-width)
                          (* total-height))
        target-height (if max-aspect-ratio
                        (min target-height
                             (* max-aspect-ratio target-width))
                        target-height)
        scale (min (/ target-width total-width)
                   (/ target-height total-height))]
    {:target-width target-width
     :target-height target-height
     :transform (str
                 "translate(" (/ target-width 2) "," (/ target-height 2) ")"
                 "scale(" scale "," scale ")"
                 "translate("
                 (- 0 (/ total-width 2) min-x) ","
                 (- 0 (/ total-height 2) min-y) ")")}))

(defn get-used-fonts [context]
  (let [num-ornaments (interface/get-list-size (c/++ context :ornaments :elements))]
    ;; TODO: might have to be smarter here to only look into mottos,
    ;; but it should work if there's no :ribbon :segments
    (->> (for [i (range num-ornaments)
               j (range (interface/get-list-size (update context :path
                                                         conj
                                                         :ornaments
                                                         :elements
                                                         i
                                                         :ribbon
                                                         :segments)))]
           (interface/get-sanitized-data (update context :path
                                                 conj
                                                 :ornaments
                                                 :elements
                                                 i
                                                 :ribbon
                                                 :segments
                                                 j
                                                 :font)))
         (filter identity)
         (into #{}))))

(defn achievement [{:keys [short-url
                           svg-export?] :as context}]
  (let [short-url-font :deja-vu-sans
        coat-of-arms-angle (interface/render-option :coat-of-arms-angle context)
        scope (interface/render-option :scope context)
        coa-angle-rad-abs (-> coat-of-arms-angle
                              Math/abs
                              (* Math/PI)
                              (/ 180))
        coa-angle-counter-rad-abs (- (/ Math/PI 2)
                                     coa-angle-rad-abs)
        {coat-of-arms :result
         environment :environment} (coat-of-arms
                                    (c/++ context :coat-of-arms)
                                    100)
        {coat-of-arms-width :width
         coat-of-arms-height :height} environment
        {helms-result-below-shield :result-below-shield
         helms-result-above-shield :result-above-shield
         helms-width :width
         helms-height :height} (if (= scope :coat-of-arms)
                                 {:width 0
                                  :height 0
                                  :result nil}
                                 (helms
                                  (c/++ context :helms)
                                  100))

        short-arm (* coat-of-arms-width (Math/cos coa-angle-rad-abs))
        long-arm (* coat-of-arms-height (Math/cos coa-angle-counter-rad-abs))
        [rotated-min-x
         rotated-max-x] (if (neg? coat-of-arms-angle)
                          [(- long-arm)
                           short-arm]
                          [(- coat-of-arms-width short-arm)
                           (+ coat-of-arms-width long-arm)])
        rotated-height (+ (* coat-of-arms-width (Math/sin coa-angle-rad-abs))
                          (* coat-of-arms-height (Math/sin coa-angle-counter-rad-abs)))
        rotated? (not (zero? coat-of-arms-angle))
        helm-position (cond
                        (neg? coat-of-arms-angle) (v/v (- (/ coat-of-arms-width 2)) 0)
                        (pos? coat-of-arms-angle) (v/v (/ coat-of-arms-width 2) 0)
                        :else (v/v 0 0))
        helms-bounding-box (bounding-box/min-max-x-y [(-> helm-position
                                                          (v/add (v/v (/ coat-of-arms-width 2) 0))
                                                          (v/add (v/v (- (/ helms-width 2))
                                                                      (- helms-height))))
                                                      (-> helm-position
                                                          (v/add (v/v (/ coat-of-arms-width 2) 0))
                                                          (v/add (v/v (/ helms-width 2)
                                                                      0)))])
        coat-of-arms-bounding-box (if rotated?
                                    [rotated-min-x rotated-max-x
                                     0 rotated-height]
                                    [0 coat-of-arms-width
                                     0 coat-of-arms-height])
        coa-and-helms-bounding-box (bounding-box/combine
                                    (cond-> [coat-of-arms-bounding-box]
                                      helms-result-below-shield (conj helms-bounding-box)))

        {ornaments-result-below-shield :result-below-shield
         ornaments-result-above-shield :result-above-shield
         ornaments-bounding-box :bounding-box} (if (#{:coat-of-arms
                                                      :coat-of-arms-and-helm} scope)
                                                 {:bounding-box [0 0 0 0]}
                                                 (ornaments
                                                  (c/++ context :ornaments)
                                                  coat-of-arms-bounding-box))

        used-fonts (cond-> (get-used-fonts context)
                     short-url (conj short-url-font))

        achievement-bounding-box (bounding-box/combine
                                  (cond-> [coa-and-helms-bounding-box]
                                    ;; TODO: restore this functionality, resize the achievement based on mottos
                                    ;;mottos-result-below-shield (conj mottos-bounding-box)
                                    ornaments-result-below-shield (conj ornaments-bounding-box)))

        target-width 1000
        {achievement-width :target-width
         achievement-height :target-height
         achievement-transform :transform} (transform-bounding-box
                                            achievement-bounding-box
                                            target-width
                                            :max-aspect-ratio 1.5)
        margin 10
        font-size 20
        document-width (-> achievement-width (+ (* 2 margin)))
        document-height (-> achievement-height (+ (* 2 margin)) (+ 20)
                            (cond-> short-url
                              (+ font-size margin)))]
    [:svg (merge
           {:viewBox (str "0 0 " document-width " " document-height)}
           (if svg-export?
             {:xmlns "http://www.w3.org/2000/svg"
              :version "1.1"
              :width document-width
              :height document-height}
             {:style {:width "100%"}
              :preserveAspectRatio "xMidYMin meet"}))
     (when svg-export?
       [output/embed-fonts used-fonts])
     [:g {:transform (str "translate(" margin "," margin ")")}
      [:g {:transform achievement-transform
           :style {:transition "transform 0.5s"}}

       (when ornaments-result-below-shield
         [:g
          ornaments-result-below-shield])

       (when helms-result-below-shield
         [:g {:transform (str "translate(" (v/->str helm-position) ")")
              :style {:transition "transform 0.5s"}}
          helms-result-below-shield])

       [:g {:transform (cond
                         (neg? coat-of-arms-angle) (str "rotate(" (- coat-of-arms-angle) ")")
                         (pos? coat-of-arms-angle) (str "translate(" coat-of-arms-width "," 0 ")"
                                                        "rotate(" (- coat-of-arms-angle) ")"
                                                        "translate(" (- coat-of-arms-width) "," 0 ")")
                         :else nil)
            :style {:transition "transform 0.5s"}}
        coat-of-arms]

       (when helms-result-above-shield
         [:g {:transform (str "translate(" (v/->str helm-position) ")")
              :style {:transition "transform 0.5s"}}
          helms-result-above-shield])

       (when ornaments-result-above-shield
         [:g
          ornaments-result-above-shield])]]

     (when short-url
       [:text {:x margin
               :y (- document-height
                     margin)
               :text-align "start"
               :fill "#888"
               :style {:font-family (font/css-string short-url-font)
                       :font-size font-size}}
        short-url])]))
