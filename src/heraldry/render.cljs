(ns heraldry.render
  (:require [clojure.string :as s]
            [heraldry.backend.output :as output]
            [heraldry.vector.catmullrom :as catmullrom]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.vector.filter :as filter]
            [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.texture :as texture]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.vector.core :as v]
            [heraldry.font :as font]
            [heraldry.interface :as interface]
            [heraldry.ribbon :as ribbon]
            [heraldry.vector.svg :as svg]
            [heraldry.util :as util]))

(defn coat-of-arms [path width
                    {:keys
                     [svg-export?
                      metadata-path
                      texture-link] :as context}]
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
        shield (escutcheon/field escutcheon)
        environment (-> (environment/transform-to-width shield width)
                        (cond->
                         squiggly? (update :shape svg/squiggly-path)))
        mask-id (util/id "mask")
        texture-id (util/id "texture")
        shiny-id (util/id "shiny")
        texture-link (or texture-link (texture/full-path texture))]
    {:environment environment
     :result [:g {:filter (when escutcheon-shadow?
                            "url(#shadow)")}
              (when metadata-path
                [metadata/attribution metadata-path :arms context])
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
                [:path {:d (:shape environment)
                        :fill "#fff"
                        :stroke "none"}]]]
              [:g {(if svg-export?
                     :mask
                     :clip-path) (str "url(#" mask-id ")")}
               [:g {:filter (when texture-link (str "url(#" texture-id ")"))}
                [:g {:filter (when shiny?
                               (str "url(#" shiny-id ")"))}
                 [:path {:d (:shape environment)
                         :fill "#f0f0f0"}]
                 [field-shared/render (conj path :field)
                  environment
                  (-> context
                      (dissoc :metadata-path)
                      (assoc :root-escutcheon escutcheon))]]]]
              (when (or escutcheon-outline?
                        outline?)
                [:g (outline/style context)
                 [:path {:d (:shape environment)}]])]}))

(defn helm [path environment context]
  (let [components-path (conj path :components)
        num-components (interface/get-list-size components-path context)]
    [:<>
     (doall
      (for [idx (range num-components)]
        ^{:key idx}
        [interface/render-component
         (conj components-path idx)
         path environment
         context]))]))

(defn helms [path width context]
  (let [elements-path (conj path :elements)
        num-helms (interface/get-list-size elements-path context)]
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
         :result [:g
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
                       [helm (conj elements-path idx) helm-environment context])))]}))))

(defn ribbon [path
              tincture-foreground
              tincture-background
              tincture-text
              context & {:keys [outline-thickness]
                         :or {outline-thickness 1}}]
  (let [thickness (interface/get-sanitized-data (conj path :thickness) context)
        edge-angle (interface/get-sanitized-data (conj path :edge-angle) context)
        end-split (interface/get-sanitized-data (conj path :end-split) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points-path (conj path :points)
        segments-path (conj path :segments)
        points (interface/get-raw-data points-path context)
        segments (interface/get-raw-data segments-path context)
        {:keys [curves edge-vectors]} (ribbon/generate-curves points edge-angle)
        num-curves (count curves)
        foreground-colour (tincture/pick tincture-foreground context)
        background-colour (if (= tincture-background :none)
                            (svg/darken-colour foreground-colour)

                            (tincture/pick tincture-background context))
        text-colour (tincture/pick tincture-text context)]
    [:<>
     (doall
      (for [[idx partial-curve] (->> curves
                                     (map-indexed vector)
                                     (sort-by (fn [[idx _]]
                                                [(-> segments
                                                     (get idx)
                                                     :z-index
                                                     (or 1000))
                                                 idx])))]
        (let [top-edge (catmullrom/curve->svg-path-relative partial-curve)
              [first-edge-vector second-edge-vector] (get edge-vectors idx)
              first-edge-vector (v/* first-edge-vector thickness)
              second-edge-vector (v/* second-edge-vector thickness)
              full-path (cond
                          (or (zero? end-split)
                              (< 0 idx (dec num-curves))) (str top-edge
                                                               (catmullrom/svg-line-to second-edge-vector)
                                                               (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                               (catmullrom/svg-line-to (v/* first-edge-vector -1)))
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
                                                 (catmullrom/svg-line-to second-edge-vector)
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
                                                              (catmullrom/svg-line-to (v/* first-edge-vector -1))))
              segment-path (conj segments-path idx)
              segment-type (interface/get-raw-data (conj segment-path :type) context)
              foreground? (#{:heraldry.ribbon.segment/foreground
                             :heraldry.ribbon.segment/foreground-with-text} segment-type)
              text (some-> (interface/get-sanitized-data (conj segment-path :text) context)
                           (s/replace #"[*]" "⬪"))
              text? (and (= segment-type :heraldry.ribbon.segment/foreground-with-text)
                         (some-> text
                                 s/trim
                                 count
                                 pos?))]
          ^{:key idx}
          [:<>
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
                   spacing (interface/get-sanitized-data (conj segment-path :spacing) context)
                   offset-x (interface/get-sanitized-data (conj segment-path :offset-x) context)
                   offset-y (interface/get-sanitized-data (conj segment-path :offset-y) context)
                   font (some-> (interface/get-sanitized-data (conj segment-path :font) context)
                                font/css-string)
                   font-scale (interface/get-sanitized-data (conj segment-path :font-scale) context)
                   font-size (* font-scale thickness)
                   spacing (* spacing font-size)
                   text-path-start (v/+ (apply v/v (ffirst partial-curve))
                                        (v/* first-edge-vector (- 0.6 offset-y)))
                   text-path-end (v/+ (apply v/v (last (last partial-curve)))
                                      (v/* second-edge-vector (- 0.6 offset-y)))
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
                         :d (str "M " (:x text-path-start) "," (:y text-path-start) text-path)}]]
                [:textPath {:href (str "#" path-id)
                            :alignment-baseline "middle"
                            :method "align"
                            :lengthAdjust "spacing"
                            :letter-spacing spacing
                            :startOffset (str (+ 50 (* offset-x 100)) "%")}
                 text]]))])))]))

(defn motto [path {:keys [width] :as environment} context]
  (let [ribbon-path (conj path :ribbon)
        points (interface/get-raw-data (conj ribbon-path :points) context)]
    (if points
      (let [origin-point (interface/get-sanitized-data (conj path :origin :point) context)
            tincture-foreground (interface/get-sanitized-data (conj path :tincture-foreground) context)
            tincture-background (interface/get-sanitized-data (conj path :tincture-background) context)
            tincture-text (interface/get-sanitized-data (conj path :tincture-text) context)
            offset-x (interface/get-sanitized-data (conj path :origin :offset-x) context)
            offset-y (interface/get-sanitized-data (conj path :origin :offset-y) context)
            size (interface/get-sanitized-data (conj path :geometry :size) context)
            thickness (interface/get-sanitized-data (conj ribbon-path :thickness) context)
            position (-> (-> environment :points (get origin-point))
                         (v/+ (v/v ((util/percent-of width) offset-x)
                                   (- ((util/percent-of width) offset-y)))))
            ;; TODO: not ideal, need the thickness here and need to know that the edge-vector (here
            ;; assumed to be (0 thickness) as a max) needs to be added to every point for the correct
            ;; height; could perhaps be a subscription or the ribbon function can provide it?
            ;; but then can't use the ribbon function as reagent component
            [min-x max-x
             min-y max-y] (svg/min-max-x-y (concat points
                                                   (map (partial v/+ (v/v 0 thickness)) points)))
            ribbon-width (- max-x min-x)
            shift (v/v (- (/ ribbon-width -2) min-x)
                       (case origin-point
                         :top (- max-y)
                         :bottom (- min-y)))
            target-width ((util/percent-of width) size)
            scale (/ target-width ribbon-width)
            outline-thickness (/ 1 scale)]
        {:result [:g {:transform (str "translate(" (:x position) "," (:y position) ")"
                                      "scale(" scale "," scale ")"
                                      "translate(" (:x shift) "," (:y shift) ")")}
                  [ribbon
                   ribbon-path
                   tincture-foreground
                   tincture-background
                   tincture-text
                   context :outline-thickness outline-thickness]]
         :bounding-box (let [top-left (-> (v/v min-x min-y)
                                          (v/+ shift)
                                          (v/* scale)
                                          (v/+ position))
                             bottom-right (-> (v/v max-x max-y)
                                              (v/+ shift)
                                              (v/* scale)
                                              (v/+ position))]

                         [(:x top-left) (:x bottom-right)
                          (:y top-left) (:y bottom-right)])})
      {:result nil
       :bounding-box nil})))

(defn mottos [path width height context]
  (let [elements-path (conj path :elements)
        num-mottos (interface/get-list-size elements-path context)
        motto-environment (environment/create
                           nil
                           {:bounding-box [0
                                           width
                                           0
                                           height]})
        mottos-data (vec
                     (for [idx (range num-mottos)]
                       (-> (motto (conj elements-path idx) motto-environment context)
                           (assoc :idx idx))))
        combined-bounding-box (->> mottos-data
                                   (keep :bounding-box)
                                   svg/combine-bounding-boxes)]
    {:result [:g
              (doall
               (for [{:keys [idx result]} mottos-data]
                 ^{:key idx}
                 [:<> result]))]
     :bounding-box combined-bounding-box}))

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

(defn get-used-fonts [path context]
  (let [mottos-elements-path (conj path :mottos :elements)
        num-mottos (interface/get-list-size mottos-elements-path context)]
    (->> (for [i (range num-mottos)
               j (range (interface/get-list-size (conj
                                                  mottos-elements-path
                                                  i
                                                  :ribbon
                                                  :segments) context))]
           (interface/get-sanitized-data (conj
                                          mottos-elements-path
                                          i
                                          :ribbon
                                          :segments
                                          j
                                          :font) context))
         (filter identity)
         (into #{}))))

(defn achievement [path {:keys [short-url
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
                                    (conj path :coat-of-arms)
                                    100
                                    context)
        {coat-of-arms-width :width
         coat-of-arms-height :height} environment
        {helms-result :result
         helms-width :width
         helms-height :height} (if (= scope :coat-of-arms)
                                 {:width 0
                                  :height 0
                                  :result nil}
                                 (helms
                                  (conj path :helms)
                                  100
                                  context))

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
        helms-bounding-box (svg/min-max-x-y [(-> helm-position
                                                 (v/+ (v/v (/ coat-of-arms-width 2) 0))
                                                 (v/+ (v/v (- (/ helms-width 2))
                                                           (- helms-height))))
                                             (-> helm-position
                                                 (v/+ (v/v (/ coat-of-arms-width 2) 0))
                                                 (v/+ (v/v (/ helms-width 2)
                                                           0)))])
        coat-of-arms-bounding-box (if rotated?
                                    [rotated-min-x rotated-max-x
                                     0 rotated-height]
                                    [0 coat-of-arms-width
                                     0 coat-of-arms-height])
        coa-and-helms-bounding-box (svg/combine-bounding-boxes
                                    (cond-> [coat-of-arms-bounding-box]
                                      helms-result (conj helms-bounding-box)))
        target-width 1000
        {coa-and-helms-width :target-width
         coa-and-helms-height :target-height
         coa-and-helms-transform :transform} (transform-bounding-box
                                              coa-and-helms-bounding-box
                                              target-width)
        {mottos-result :result
         mottos-bounding-box :bounding-box} (if (= scope :coat-of-arms)
                                              {:bounding-box [0 0 0 0]
                                               :result nil
                                               :used-fonts #{}}
                                              (mottos
                                               (conj path :mottos)
                                               coa-and-helms-width
                                               coa-and-helms-height
                                               context))

        used-fonts (cond-> (get-used-fonts path context)
                     short-url (conj short-url-font))

        achievement-bounding-box (svg/combine-bounding-boxes
                                  (cond-> [[0 coa-and-helms-width
                                            0 coa-and-helms-height]]
                                    mottos-result (conj mottos-bounding-box)))
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
       [:g {:transform coa-and-helms-transform
            :style {:transition "transform 0.5s"}}
        [:g {:transform (cond
                          (neg? coat-of-arms-angle) (str "rotate(" (- coat-of-arms-angle) ")")
                          (pos? coat-of-arms-angle) (str "translate(" coat-of-arms-width "," 0 ")"
                                                         "rotate(" (- coat-of-arms-angle) ")"
                                                         "translate(" (- coat-of-arms-width) "," 0 ")")
                          :else nil)
             :style {:transition "transform 0.5s"}}
         coat-of-arms]
        (when helms-result
          [:g {:transform (str "translate(" (:x helm-position) "," (:y helm-position) ")")
               :style {:transition "transform 0.5s"}}
           helms-result])]

       (when mottos-result
         [:g
          mottos-result])]]

     (when short-url
       [:text {:x margin
               :y (- document-height
                     margin)
               :text-align "start"
               :fill "#888"
               :style {:font-family (font/css-string short-url-font)
                       :font-size font-size}}
        short-url])]))
