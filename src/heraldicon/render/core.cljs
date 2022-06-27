(ns heraldicon.render.core
  (:require
   [heraldicon.context :as c]
   [heraldicon.font :as font]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.render.coat-of-arms :as coat-of-arms]
   [heraldicon.render.helms :as helms]
   [heraldicon.render.outline :as outline]
   [heraldicon.render.ribbon :as ribbon]))

(defn motto [{:keys [environment
                     self-below-shield?
                     render-pass-below-shield?]
              :as context}]
  (let [{:keys [width height]} environment
        points (interface/get-raw-data (c/++ context :ribbon :points))]
    (if (and (= (boolean self-below-shield?)
                (boolean render-pass-below-shield?))
             points)
      (let [anchor-point (interface/get-sanitized-data (c/++ context :anchor :point))
            tincture-foreground (interface/get-sanitized-data (c/++ context :tincture-foreground))
            tincture-background (interface/get-sanitized-data (c/++ context :tincture-background))
            tincture-text (interface/get-sanitized-data (c/++ context :tincture-text))
            offset-x (interface/get-sanitized-data (c/++ context :anchor :offset-x))
            offset-y (interface/get-sanitized-data (c/++ context :anchor :offset-y))
            size (interface/get-sanitized-data (c/++ context :geometry :size))
            thickness (interface/get-sanitized-data (c/++ context :ribbon :thickness))
            position (v/add (-> environment :points (get anchor-point))
                            (v/Vector. ((math/percent-of width) offset-x)
                                       (- ((math/percent-of height) offset-y))))
            ;; TODO: not ideal, need the thickness here and need to know that the edge-vector (here
            ;; assumed to be (0 thickness) as a max) needs to be added to every point for the correct
            ;; height; could perhaps be a subscription or the ribbon function can provide it?
            ;; but then can't use the ribbon function as reagent component
            {:keys [min-x max-x
                    min-y max-y]} (bb/from-points
                                   (concat points
                                           (map (partial v/add (v/Vector. 0 thickness)) points)))
            ribbon-width (- max-x min-x)
            ribbon-height (- max-y min-y)
            target-width ((math/percent-of width) size)
            scale (/ target-width ribbon-width)
            outline-thickness (/ outline/stroke-width
                                 2
                                 scale)]
        {:result [:g {:transform (str "translate(" (v/->str position) ")"
                                      "scale(" scale "," scale ")"
                                      "translate(" (v/->str (-> (v/Vector. ribbon-width ribbon-height)
                                                                (v/div 2)
                                                                (v/add (v/Vector. min-x min-y))
                                                                (v/mul -1))) ")")}
                  [ribbon/render
                   (c/++ context :ribbon)
                   tincture-foreground
                   tincture-background
                   tincture-text
                   :outline-thickness outline-thickness]]
         :bounding-box (bb/from-points
                        [(-> (v/Vector. min-x min-y)
                             (v/mul scale)
                             (v/add position))
                         (-> (v/Vector. max-x max-y)
                             (v/mul scale)
                             (v/add position))])})
      {:result nil
       :bounding-box nil})))

(defn- ornaments-elements [context & {:keys [below-shield?]}]
  (into [:<>]
        (map (fn [[idx self-below-shield?]]
               (let [updated-context (-> context
                                         (c/++ idx)
                                         (assoc :auto-resize? false
                                                :self-below-shield? self-below-shield?
                                                :render-pass-below-shield? below-shield?))
                     motto? (interface/motto? updated-context)]
                 ^{:key idx}
                 [:<>
                  (if motto?
                    (:result (motto updated-context))
                    [interface/render-component updated-context])])))
        (interface/get-element-indices context)))

(defn ornaments [context coa-bounding-box]
  (let [{bb-min-x :min-x
         bb-max-x :max-x
         bb-min-y :min-y
         bb-max-y :max-y} coa-bounding-box
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

       :bounding-box (bb/BoundingBox.
                      ornaments-left
                      (+ ornaments-left ornaments-width)
                      ornaments-top
                      (+ ornaments-top ornaments-height))}
      {:bounding-box (bb/BoundingBox. 0 0 0 0)})))

(defn- transform-bounding-box [^BoundingBox {:keys [min-x max-x min-y max-y]}
                               ^js/Number target-width & {:keys [^js/Number max-aspect-ratio]}]
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

(defn- get-used-fonts [context]
  (let [num-ornaments (interface/get-list-size (c/++ context :ornaments :elements))]
    ;; TODO: might have to be smarter here to only look into mottos,
    ;; but it should work if there's no :ribbon :segments
    (into #{}
          (filter identity)
          (for [i (range num-ornaments)
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
                                                  :font))))))

(defn achievement [{:keys [short-url
                           svg-export?
                           target-width
                           target-height
                           embed-fonts] :as context}]
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
         environment :environment} (coat-of-arms/render
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
                                 (helms/render
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
                        (neg? coat-of-arms-angle) (v/Vector. (- (/ coat-of-arms-width 2)) 0)
                        (pos? coat-of-arms-angle) (v/Vector. (/ coat-of-arms-width 2) 0)
                        :else v/zero)
        helms-bounding-box (bb/from-points
                            [(-> helm-position
                                 (v/add (v/Vector. (/ coat-of-arms-width 2) 0))
                                 (v/add (v/Vector. (- (/ helms-width 2))
                                                   (- helms-height))))
                             (-> helm-position
                                 (v/add (v/Vector. (/ coat-of-arms-width 2) 0))
                                 (v/add (v/Vector. (/ helms-width 2)
                                                   0)))])
        coat-of-arms-bounding-box (if rotated?
                                    (bb/BoundingBox. rotated-min-x rotated-max-x
                                                     0 rotated-height)
                                    (bb/BoundingBox. 0 coat-of-arms-width
                                                     0 coat-of-arms-height))
        coa-and-helms-bounding-box (cond-> coat-of-arms-bounding-box
                                     helms-result-below-shield (bb/combine helms-bounding-box))

        {ornaments-result-below-shield :result-below-shield
         ornaments-result-above-shield :result-above-shield
         ornaments-bounding-box :bounding-box} (if (#{:coat-of-arms
                                                      :coat-of-arms-and-helm} scope)
                                                 {:bounding-box (bb/BoundingBox. 0 0 0 0)}
                                                 (ornaments
                                                  (c/++ context :ornaments)
                                                  coat-of-arms-bounding-box))

        used-fonts (cond-> (get-used-fonts context)
                     short-url (conj short-url-font))

        achievement-bounding-box (cond-> coa-and-helms-bounding-box
                                   ;; TODO: restore this functionality, resize the achievement based on mottos
                                   ;; mottos-result-below-shield (bb/combine mottos-bounding-box)
                                   ornaments-result-below-shield (bb/combine ornaments-bounding-box))

        achievement-width 1000
        {achievement-width :target-width
         achievement-height :target-height
         achievement-transform :transform} (transform-bounding-box
                                            achievement-bounding-box
                                            achievement-width
                                            :max-aspect-ratio 1.5)
        margin 10
        font-size 20
        result-width (+ achievement-width (* 2 margin))
        result-height (-> (+ achievement-height
                             (* 2 margin)
                             20)
                          (cond->
                            short-url (+ font-size margin)))

        [document-width
         document-height
         document-scale] (if (and svg-export?
                                  (or target-width
                                      target-height))
                           (let [scale-width (when target-width
                                               (/ target-width result-width))
                                 scale-height (when target-height
                                                (/ target-height result-height))
                                 scale (or (when (and scale-width scale-height)
                                             (min scale-width scale-height))
                                           scale-width
                                           scale-height)]
                             [(* result-width scale)
                              (* result-height scale)
                              scale])
                           [result-width
                            result-height
                            1])]
    [:svg (merge
           {:viewBox (str "0 0 " document-width " " document-height)}
           (if svg-export?
             {:xmlns "http://www.w3.org/2000/svg"
              :version "1.1"
              :width document-width
              :height document-height}
             {:style {:width "100%"}
              :preserveAspectRatio "xMidYMin meet"}))
     (when (and svg-export?
                embed-fonts)
       [embed-fonts used-fonts])
     [:g {:transform (str "scale(" document-scale "," document-scale ")")}
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
           ornaments-result-above-shield])]]]

     (when short-url
       [:text {:x margin
               :y (- document-height
                     margin)
               :text-align "start"
               :fill "#888"
               :style {:font-family (font/css-string short-url-font)
                       :font-size font-size}}
        short-url])]))

(defn charge-standalone [{:keys [svg-export?
                                 target-width
                                 target-height
                                 embed-fonts] :as context}]
  (let [shield (escutcheon/field :rectangle nil nil nil nil nil)
        environment (environment/transform-to-width shield 100)
        [result-width result-height] [(:width environment)
                                      (:height environment)]
        target-width (or target-width 1000)
        [document-width
         document-height
         document-scale] (if (and svg-export?
                                  (or target-width
                                      target-height))
                           (let [scale-width (when target-width
                                               (/ target-width result-width))
                                 scale-height (when target-height
                                                (/ target-height result-height))
                                 scale (or (when (and scale-width scale-height)
                                             (min scale-width scale-height))
                                           scale-width
                                           scale-height)]
                             [(* result-width scale)
                              (* result-height scale)
                              scale])
                           [result-width
                            result-height
                            1])
        used-fonts (into #{}
                         (keep (fn [j]
                                 (interface/get-sanitized-data (c/++ context :segments j :font))))
                         (range (interface/get-list-size (c/++ context :segments))))]
    [:svg (merge
           {:viewBox (str "0 0 " document-width " " document-height)}
           (if svg-export?
             {:xmlns "http://www.w3.org/2000/svg"
              :version "1.1"
              :width document-width
              :height document-height}
             {:style {:width "100%"}
              :preserveAspectRatio "xMidYMin meet"}))
     (when (and svg-export?
                embed-fonts)
       [embed-fonts used-fonts])
     [:g {:transform (str "scale(" document-scale "," document-scale ")")}
      [interface/render-component (assoc context :environment environment)]]]))
