(ns heraldicon.heraldry.charge.other
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]
   [heraldicon.context :as c]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.core :as svg]
   [heraldicon.svg.metadata :as svg.metadata]
   [heraldicon.util.colour :as colour]
   [heraldicon.util.uid :as uid]))

(defmethod charge.interface/options :heraldry.charge.type/other [context]
  (-> (charge.shared/options context)
      (assoc :tincture {:eyed {:type :option.type/choice
                               :choices tincture/choices
                               :default :argent
                               :ui/label :string.charge.tincture-modifier/eyed}
                        :toothed {:type :option.type/choice
                                  :choices tincture/choices
                                  :default :argent
                                  :ui/label :string.charge.tincture-modifier/toothed}
                        :shadow {:type :option.type/range
                                 :min 0
                                 :max 1
                                 :default 1
                                 :ui/label :string.option/shadow
                                 :ui/step 0.01}
                        :highlight {:type :option.type/range
                                    :min 0
                                    :max 1
                                    :default 1
                                    :ui/label :string.option/highlight
                                    :ui/step 0.01}
                        :ui/label :string.entity/tinctures
                        :ui/element :ui.element/tincture-modifiers})
      (assoc :ignore-layer-separator?
             {:type :option.type/boolean
              :default (->> context :path (some #{:coat-of-arms}))
              :ui/label :string.option/ignore-layer-separator?
              :ui/tooltip :string.tooltip/ignore-layer-separator?})))

(defn- placeholder-colour-modifier [placeholder-colours colour]
  (attributes/tincture-modifier (get placeholder-colours colour)))

(defn- placeholder-colour-qualifier [placeholder-colours colour]
  (attributes/tincture-modifier-qualifier (get placeholder-colours colour)))

(def ^:private remove-outlines
  (memoize
   (fn remove-outlines [data placeholder-colours]
     (walk/postwalk #(if (and (vector? %)
                              (->> % first (get #{:stroke :fill :stop-color}))
                              (or (-> % second (s/starts-with? "url"))
                                  (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) (= :outline))))
                       [(first %) "none"]
                       %)
                    data))))

(def ^:private remove-shading
  (memoize
   (fn remove-shading [data placeholder-colours]
     (walk/postwalk #(if (and (vector? %)
                              (->> % first (get #{:stroke :fill :stop-color}))
                              (or (-> % second (s/starts-with? "url"))
                                  (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) #{:shadow :highlight})))
                       [(first %) "none"]
                       %)
                    data))))

(def ^:private remove-layer-separator
  (memoize
   (fn remove-layer-separator [data placeholder-colours]
     (walk/postwalk #(cond
                       (and (vector? %)
                            (->> % first (get #{:stroke :fill :stop-color}))
                            (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) #{:layer-separator}))
                       [(first %) "none"]
                       :else %)
                    data))))

;; TODO: the function here prevents efficient memoization, because the calling
;; context uses anonymous functions that use closures
(defn- replace-colours [data function]
  (walk/postwalk #(if (and (vector? %)
                           (-> % second string?)
                           (->> % first (get #{:stroke :fill :stop-color}))
                           (-> % second (not= "none")))
                    [(first %) (function (second %))]
                    %)
                 data))

(defn- highlight-colour [colour highlight-colours]
  (if (-> colour colour/normalize highlight-colours)
    "#00ff00"
    (colour/desaturate colour)))

(def ^:private set-layer-separator-opacity
  (memoize
   (fn set-layer-separator-opacity [data layer-separator-colours opacity]
     (if (seq layer-separator-colours)
       (let [layer-separator-colours (set layer-separator-colours)]
         (walk/postwalk (fn [v]
                          (if (map? v)
                            (loop [v v
                                   [[attribute opacity-attribute] & rest] [[:stroke :stroke-opacity]
                                                                           [:fill :fill-opacity]
                                                                           [:stop-color :stop-opacity]]]
                              (let [new-v (cond-> v
                                            (some-> (get v attribute)
                                                    colour/normalize
                                                    layer-separator-colours) (->
                                                                               (assoc opacity-attribute opacity)
                                                                               (cond->
                                                                                 (:opacity v) (assoc :opacity opacity))))]
                                (if (seq rest)
                                  (recur new-v rest)
                                  new-v)))
                            v))
                        data))
       data))))

(defn- get-replacement [kind provided-placeholder-colours]
  (let [replacement (get provided-placeholder-colours kind)]
    (when-not (or (nil? replacement)
                  (= replacement :none))
      replacement)))

(def ^:private make-mask
  (memoize
   (fn make-mask [data placeholder-colours provided-placeholder-colours
                  outline-mode hide-lower-layer?]
     (let [mask (replace-colours
                 data
                 (fn [colour]
                   (if (s/starts-with? colour "url")
                     "none"
                     (let [colour-lower (colour/normalize colour)
                           kind (placeholder-colour-modifier placeholder-colours colour-lower)
                           replacement (get-replacement kind provided-placeholder-colours)]
                       (cond
                         (or (= kind :keep)
                             (and (= outline-mode :remove)
                                  (= kind :outline))
                             replacement) "#fff"
                         (and (= kind :layer-separator)
                              (not hide-lower-layer?)) "none"
                         (and (= kind :layer-separator)
                              hide-lower-layer?) "#000"
                         :else "#000")))))
           mask-base (replace-colours
                      data
                      (fn [colour]
                        (if (s/starts-with? colour "url")
                          "none"
                          (let [colour-lower (colour/normalize colour)
                                kind (placeholder-colour-modifier placeholder-colours colour-lower)]
                            (cond
                              (and (= kind :layer-separator)
                                   (not hide-lower-layer?)) "none"
                              (and (= kind :layer-separator)
                                   hide-lower-layer?) "#000"
                              :else "#fff")))))
           mask-inverted (replace-colours
                          data
                          (fn [colour]
                            (if (s/starts-with? colour "url")
                              "none"
                              (let [colour-lower (colour/normalize colour)
                                    kind (placeholder-colour-modifier placeholder-colours colour-lower)
                                    replacement (get-replacement kind provided-placeholder-colours)]
                                (cond
                                  (or (= kind :keep)
                                      (and (not (#{:primary} outline-mode))
                                           (= kind :outline))
                                      replacement) "#000"
                                  (and (= kind :layer-separator)
                                       (not hide-lower-layer?)) "none"
                                  (and (= kind :layer-separator)
                                       hide-lower-layer?) "#000"
                                  :else "#fff")))))]
       [mask mask-inverted mask-base]))))

(defn- colours-for-modifier [placeholder-colours modifier]
  (->> placeholder-colours
       (keep (fn [[colour placeholder]]
               (when (= placeholder modifier)
                 (colour/normalize colour))))
       set))

(derive :heraldry.charge.type/other :heraldry/charge)

(defmethod interface/properties :heraldry.charge.type/other [{:keys [load-charge-data] :as context}]
  (let [data (interface/get-raw-data (c/++ context :data))
        variant (interface/get-raw-data (c/++ context :variant))
        charge-entity (or data (when variant (load-charge-data variant)))
        charge-data (-> charge-entity :data :edn-data)
        base-width (js/parseFloat (-> charge-data :width (or "1")))
        base-height (js/parseFloat (-> charge-data :height (or "1")))
        unadjusted-charge (svg/strip-clip-paths (:data charge-data))]
    (charge.shared/process-shape
     context
     {:base-shape [["M" (- (/ base-width 2)) (- (/ base-height 2))
                    "h" base-width
                    "v" base-height
                    "h" (- base-width)
                    "z"]]
      :base-width base-width
      :base-height base-height
      :other? true
      :charge-entity charge-entity
      :unadjusted-charge unadjusted-charge})))

(defn render [{:keys [self-below-shield? render-pass-below-shield?
                      ui-show-colours
                      select-component-fn
                      svg-export?
                      preview-original?
                      charge-preview?] :as context}
              {:keys [anchor-point scale-x scale-y angle top-left unadjusted-charge
                      charge-entity]}]
  (let [charge-data (-> charge-entity :data :edn-data)
        placeholder-colours (-> charge-entity :data :colours)
        layer-separator-colours (colours-for-modifier placeholder-colours :layer-separator)
        ignore-layer-separator? (interface/get-sanitized-data (c/++ context :ignore-layer-separator?))]
    (if (and charge-data
             ;; in order to require rendering, we either have
             ;; to be located in the right render pass
             ;; OR have some layer separator in the charge
             (or (= (boolean self-below-shield?)
                    (boolean render-pass-below-shield?))
                 (and (not ignore-layer-separator?)
                      (seq layer-separator-colours))))
      (let [highlight-colours? (and preview-original?
                                    (seq ui-show-colours))
            ui-show-colours (set ui-show-colours)
            ;; not all tinctures have their own options, but some do, so
            ;; override those with the sanitized values
            tincture (merge
                      (interface/get-raw-data (c/++ context :tincture))
                      (interface/get-sanitized-data (c/++ context :tincture)))
            outline-mode (if (or (interface/render-option :outline? context)
                                 (= (interface/render-option :mode context)
                                    :hatching)) :keep
                             (interface/get-sanitized-data (c/++ context :outline-mode)))
            fixed-tincture (-> charge-data :fixed-tincture (or :none))
            render-field? (= fixed-tincture :none)
            landscape? (:landscape? charge-data)
            render-shadow? (and (not preview-original?)
                                (->> placeholder-colours
                                     vals
                                     (filter (fn [placeholder]
                                               (or (= placeholder :shadow)
                                                   (and (vector? placeholder)
                                                        (-> placeholder second attributes/shadow-qualifiers)))))
                                     first)
                                (:shadow tincture)
                                (pos? (:shadow tincture)))
            shadow-mask-id (when render-shadow?
                             (uid/generate "mask"))
            shadow-helper-mask-id (when render-shadow?
                                    (uid/generate "mask"))
            render-highlight? (and (not preview-original?)
                                   (->> placeholder-colours
                                        vals
                                        (filter (fn [placeholder]
                                                  (or (= placeholder :highlight)
                                                      (and (vector? placeholder)
                                                           (-> placeholder second attributes/highlight-qualifiers)))))
                                        first)
                                   (:highlight tincture)
                                   (pos? (:highlight tincture)))
            highlight-mask-id (when render-highlight?
                                (uid/generate "mask"))
            highlight-helper-mask-id (when render-highlight?
                                       (uid/generate "mask"))
            adjusted-charge (when-not preview-original?
                              (-> unadjusted-charge
                                  (set-layer-separator-opacity
                                   layer-separator-colours
                                   (if highlight-colours?
                                     0.5
                                     1))
                                  (cond->
                                    (= outline-mode :remove) (remove-outlines placeholder-colours)

                                    highlight-colours? (replace-colours
                                                        (fn [colour]
                                                          (highlight-colour
                                                           colour ui-show-colours))))))
            adjusted-charge-without-shading (when-not preview-original?
                                              (cond-> adjusted-charge
                                                (not highlight-colours?) (remove-shading placeholder-colours)))
            fimbriation-shape (-> unadjusted-charge
                                  (remove-shading placeholder-colours)
                                  (remove-layer-separator placeholder-colours))
            hide-lower-layer? (and (seq layer-separator-colours)
                                   (not ignore-layer-separator?)
                                   (not render-pass-below-shield?))
            mask-id (uid/generate "mask")
            mask-inverted-id (uid/generate "mask")
            mask-base-id (uid/generate "mask")
            [mask mask-inverted mask-base] (when-not (or preview-original?
                                                         landscape?)
                                             (make-mask adjusted-charge-without-shading
                                                        placeholder-colours
                                                        tincture
                                                        outline-mode
                                                        hide-lower-layer?))
            ;; this is the one drawn on top of the masked field version, for the tincture replacements
            ;; and outline; the primary colour has no replacement here, which might make it shine through
            ;; around the edges, to prevent that, it is specifically replaced with black
            ;; the same is true for tincture codes for which no replacement has been set, as those should
            ;; become primary
            ;; so only render things that have a replacement or explicitly are set to "keep",
            ;; other things become black for the time being
            ;; TODO: perhaps they can be removed entirely? there still is a faint dark edge in some cases,
            ;; much less than before, however, and the dark edge is less obvious than the bright one
            adjusted-charge-without-shading (set-layer-separator-opacity
                                             adjusted-charge-without-shading
                                             layer-separator-colours
                                             0)
            coloured-charge (replace-colours
                             adjusted-charge-without-shading
                             (fn [colour]
                               (let [colour-lower (colour/normalize colour)
                                     kind (placeholder-colour-modifier placeholder-colours colour-lower)
                                     replacement (get-replacement kind tincture)]
                                 (cond
                                   replacement (tincture/pick replacement context)
                                   (= kind :keep) colour
                                   :else (outline/color context)))))
            layer-separator-colour-for-shadow-highlight (if hide-lower-layer?
                                                          "#000000"
                                                          "none")
            #_#_charge-clip-path-id (uid/generate "mask")]
        [:<>
         #_(when-not svg-export?
             [:defs
              [:clipPath {:id charge-clip-path-id}
             ;; TODO: whole charge here for accurate clipping
               ]])
         [:g (when-not (or svg-export?
                           charge-preview?)
               {:on-click (when select-component-fn
                            (js-event/handled
                             #(select-component-fn context)))
                :style {:cursor "pointer"}
                #_#_:clip-path (str "url(#" charge-clip-path-id ")")})
          [:defs
           (when render-shadow?
             [:mask {:id shadow-mask-id}
              [:defs
               [:mask {:id shadow-helper-mask-id}
                (-> adjusted-charge-without-shading
                    (replace-colours
                     (fn [colour]
                       (if (s/starts-with? colour "url")
                         "none"
                         (let [colour-lower (colour/normalize colour)
                               kind (placeholder-colour-modifier placeholder-colours colour-lower)]
                           (case kind
                             :outline "#000000"
                             :shadow "none"
                             :highlight "none"
                             :layer-separator layer-separator-colour-for-shadow-highlight
                             "#ffffff")))))
                    svg/make-unique-ids)]]
              [:g {:mask (str "url(#" shadow-helper-mask-id ")")}
               (-> adjusted-charge
                   (replace-colours
                    (fn [colour]
                      (if (s/starts-with? colour "url")
                        colour
                        (let [colour-lower (colour/normalize colour)
                              kind (placeholder-colour-modifier placeholder-colours colour-lower)
                              qualifier (placeholder-colour-qualifier placeholder-colours colour-lower)
                              specific (attributes/shadow-qualifiers qualifier)]
                          (cond
                            specific specific
                            (= kind :shadow) "#ffffff"
                            (= kind :highlight) "none"
                            (= kind :layer-separator) layer-separator-colour-for-shadow-highlight
                            :else "#000000")))))
                   svg/make-unique-ids)]])
           (when render-highlight?
             [:mask {:id highlight-mask-id}
              [:defs
               [:mask {:id highlight-helper-mask-id}
                (-> adjusted-charge-without-shading
                    (replace-colours
                     (fn [colour]
                       (if (s/starts-with? colour "url")
                         "none"
                         (let [colour-lower (colour/normalize colour)
                               kind (placeholder-colour-modifier placeholder-colours colour-lower)]
                           (case kind
                             :outline "#000000"
                             :shadow "none"
                             :highlight "none"
                             :layer-separator layer-separator-colour-for-shadow-highlight
                             "#ffffff")))))
                    svg/make-unique-ids)]]
              [:g {:mask (str "url(#" highlight-helper-mask-id ")")}
               (-> adjusted-charge
                   (replace-colours
                    (fn [colour]
                      (if (s/starts-with? colour "url")
                        colour
                        (let [colour-lower (colour/normalize colour)
                              kind (placeholder-colour-modifier placeholder-colours colour-lower)
                              qualifier (placeholder-colour-qualifier placeholder-colours colour-lower)
                              specific (attributes/highlight-qualifiers qualifier)]
                          (cond
                            specific specific
                            (= kind :highlight) "#ffffff"
                            (= kind :shadow) "none"
                            (= kind :layer-separator) layer-separator-colour-for-shadow-highlight
                            :else "#000000")))))
                   svg/make-unique-ids)]])
           (when-not landscape?
             [:<>
              [:mask {:id mask-id}
               (svg/make-unique-ids mask)]
              [:mask {:id mask-inverted-id}
               (svg/make-unique-ids mask-inverted)]
              (when (= outline-mode :keep)
                [:mask {:id mask-base-id}
                 (svg/make-unique-ids mask-base)])])]
          (let [transform (str "translate(" (v/->str anchor-point) ")"
                               "rotate(" angle ")"
                               "scale(" scale-x "," scale-y ")"
                               "translate(" (v/->str top-left) ")")
                reverse-transform (str "translate(" (-> top-left (v/mul -1) v/->str) ")"
                                       "scale(" (/ 1 scale-x) "," (/ 1 scale-y) ")"
                                       "rotate(" (- angle) ")"
                                       "translate(" (-> anchor-point (v/mul -1) v/->str) ")")]
            [:g {:transform transform}
             [render/shape-fimbriation context
              :fimbriation-shape fimbriation-shape
              :reverse-transform reverse-transform
              :scale (/ 1 scale-x)]
             (cond
               preview-original? (cond-> (svg/make-unique-ids unadjusted-charge)
                                   highlight-colours? (replace-colours
                                                       (fn [colour]
                                                         (highlight-colour
                                                          colour ui-show-colours))))
               landscape? unadjusted-charge
               highlight-colours? adjusted-charge
               :else [:g
                      [svg.metadata/attribution
                       {:path [:context :charge-data]
                        :charge-data charge-entity}
                       :charge]
                      (when (= outline-mode :keep)
                        [:g {:mask (str "url(#" mask-base-id ")")}
                         [:rect {:transform reverse-transform
                                 :x -500
                                 :y -500
                                 :width 1100
                                 :height 1100
                                 :fill (outline/color context)}]])

                      (when render-field?
                        [:g {:mask (str "url(#" mask-inverted-id ")")}
                         [:g {:transform reverse-transform}
                          [interface/render-component (c/++ context :field)]]])
                      [:g {:mask (str "url(#" mask-id ")")}
                       (svg/make-unique-ids coloured-charge)]
                      (when render-shadow?
                        [:g {:mask (str "url(#" shadow-mask-id ")")}
                         [:rect {:transform reverse-transform
                                 :x -500
                                 :y -500
                                 :width 1100
                                 :height 1100
                                 :fill "#001040"
                                 :style {:opacity (:shadow tincture)}}]])

                      (when render-highlight?
                        [:g {:mask (str "url(#" highlight-mask-id ")")}
                         [:rect {:transform reverse-transform
                                 :x -500
                                 :y -500
                                 :width 1100
                                 :height 1100
                                 :fill "#ffffe8"
                                 :style {:opacity (:highlight tincture)}}]])])])]])
      [:<>])))
