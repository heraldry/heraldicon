(ns heraldicon.heraldry.charge.other
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.tree :as-alias tree]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.library.arms.details :as-alias arms.details]
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.ir :as ir]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.core :as svg]
   [heraldicon.svg.metadata :as svg.metadata]
   [heraldicon.svg.path :as path]
   [heraldicon.util.cache :as cache]
   [heraldicon.util.colour :as colour]
   [heraldicon.util.walk :as util.walk]
   [re-frame.core :as rf]))

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
              :default (boolean (some #{:coat-of-arms} (:path context)))
              :ui/label :string.option/ignore-layer-separator?
              :ui/tooltip :string.tooltip/ignore-layer-separator?})))

(defn- placeholder-colour-modifier [placeholder-colours colour]
  (attributes/tincture-modifier (get placeholder-colours colour)))

(defn- placeholder-colour-qualifier [placeholder-colours colour]
  (attributes/tincture-modifier-qualifier (get placeholder-colours colour)))

(def ^:private remove-outlines
  (cache/memoize
   ::remove-outlines
   (fn remove-outlines [data placeholder-colours]
     (util.walk/postwalk #(if (and (vector? %)
                                   (->> % first (get #{:stroke :fill :stop-color}))
                                   (or (-> % second (str/starts-with? "url"))
                                       (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) (= :outline))))
                            [(first %) "none"]
                            %)
                         data))))

(def remove-shading
  (cache/memoize
   ::remove-shading
   (fn remove-shading [data placeholder-colours]
     (util.walk/postwalk #(if (and (vector? %)
                                   (->> % first (get #{:stroke :fill :stop-color}))
                                   (or (-> % second (str/starts-with? "url"))
                                       (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) #{:shadow :highlight})))
                            [(first %) "none"]
                            %)
                         data))))

(def remove-layer-separator
  (cache/memoize
   ::remove-layer-separator
   (fn remove-layer-separator [data placeholder-colours]
     (util.walk/postwalk #(cond
                            (and (vector? %)
                                 (->> % first (get #{:stroke :fill :stop-color}))
                                 (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) #{:layer-separator}))
                            [(first %) "none"]
                            :else %)
                         data))))

;; TODO: the function here prevents efficient memoization, because the calling
;; context uses anonymous functions that use closures
(defn- replace-colours [data function]
  (util.walk/postwalk #(if (and (vector? %)
                                (-> % second string?)
                                (->> % first (get #{:stroke :fill :stop-color}))
                                (-> % second (not= "none")))
                         (let [colour (second %)
                               new-colour (function colour)]
                           (if (= new-colour colour)
                             %
                             [(first %) new-colour]))
                         %)
                      data))

(defn- highlight-colour [colour highlight-colours]
  (if (-> colour colour/normalize highlight-colours)
    (let [l (-> (colour/brightness colour)
                (/ 250)
                (* 200)
                (+ 50))]
      (colour/hex-colour 0 l 0))
    (colour/desaturate colour)))

(def ^:private set-layer-separator-opacity
  (cache/memoize
   ::set-layer-separator-opacity
   (fn set-layer-separator-opacity [data layer-separator-colours opacity]
     (if (seq layer-separator-colours)
       (let [layer-separator-colours (set layer-separator-colours)]
         (util.walk/postwalk (fn [v]
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
  (cache/memoize
   ::make-mask
   (fn make-mask [data placeholder-colours provided-placeholder-colours
                  outline-mode hide-lower-layer?]
     (let [mask (replace-colours
                 data
                 (fn [colour]
                   (if (str/starts-with? colour "url")
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
                        (if (str/starts-with? colour "url")
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
                            (if (str/starts-with? colour "url")
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

(defn- def-id
  "An id for a memoized def. It counts in the cache store, which lives as long as
  the memoized defs do; uid/current-id is reset per render and could hand out an
  id a cached def already uses."
  [prefix]
  (str prefix "-" (::def-count (swap! cache/store update ::def-count (fnil inc 0)))))

(defn- mask-def [prefix content]
  (let [id (def-id prefix)]
    {:id id
     :content (svg/make-unique-ids content id)}))

;; ids and content are memoized together on the inputs, so identical masks share
;; their id, and an id can never refer to different content
(def ^:private mask-defs
  (cache/memoize
   ::mask-defs
   (fn mask-defs [data placeholder-colours provided-placeholder-colours
                  outline-mode hide-lower-layer?]
     (let [[mask mask-inverted mask-base] (make-mask data placeholder-colours provided-placeholder-colours
                                                     outline-mode hide-lower-layer?)]
       {:mask (mask-def "charge-mask" mask)
        :mask-inverted (mask-def "charge-mask-inverted" mask-inverted)
        :mask-base (mask-def "charge-mask-base" mask-base)}))))

(defn- shading-mask-colour [placeholder-colours layer-separator-colour]
  (fn [colour]
    (if (str/starts-with? colour "url")
      "none"
      (case (placeholder-colour-modifier placeholder-colours (colour/normalize colour))
        :outline "#000000"
        :shadow "none"
        :highlight "none"
        :layer-separator layer-separator-colour
        "#ffffff"))))

(defn- shading-colour [kind placeholder-colours layer-separator-colour]
  (let [[own-kind other-kind qualifiers] (case kind
                                           :shadow [:shadow :highlight attributes/shadow-qualifiers]
                                           :highlight [:highlight :shadow attributes/highlight-qualifiers])]
    (fn [colour]
      (if (str/starts-with? colour "url")
        colour
        (let [colour-lower (colour/normalize colour)
              modifier (placeholder-colour-modifier placeholder-colours colour-lower)
              specific (qualifiers (placeholder-colour-qualifier placeholder-colours colour-lower))]
          (cond
            specific specific
            (= modifier own-kind) "#ffffff"
            (= modifier other-kind) "none"
            (= modifier :layer-separator) layer-separator-colour
            :else "#000000"))))))

(def ^:private shading-mask-def
  (cache/memoize
   ::shading-mask-def
   (fn shading-mask-def [kind charge-without-shading charge placeholder-colours layer-separator-colour]
     (let [id (def-id (str "charge-" (name kind)))
           helper-id (str id "-helper")]
       {:id id
        :content [:<>
                  [:defs
                   [:mask {:id helper-id}
                    (-> charge-without-shading
                        (replace-colours (shading-mask-colour placeholder-colours layer-separator-colour))
                        (svg/make-unique-ids helper-id))]]
                  [:g {:mask (str "url(#" helper-id ")")}
                   (-> charge
                       (replace-colours (shading-colour kind placeholder-colours layer-separator-colour))
                       (svg/make-unique-ids id))]]}))))

(def ^:private clip-path-def
  (cache/memoize
   ::clip-path-def
   (fn clip-path-def [charge-shapes base-width base-height]
     {:id (def-id "charge-clip")
      :content (if (empty? charge-shapes)
                 [:rect {:x 0
                         :y 0
                         :width base-width
                         :height base-height}]
                 (for [path charge-shapes]
                   ^{:key path}
                   [:path {:d (str/join " " path)}]))})))

(defn- colours-for-modifier [placeholder-colours modifier]
  (->> placeholder-colours
       (keep (fn [[colour placeholder]]
               (when (= placeholder modifier)
                 (colour/normalize colour))))
       set))

(def ^:private clean-charge-data
  (cache/memoize
   ::clean-charge-data
   ;; sanitized here too, for charges saved before the backend did it
   (fn clean-charge-data [data]
     (-> data svg/sanitize-charge-data svg/strip-clip-paths))))

(derive :heraldry.charge.type/other :heraldry/charge)

(defmethod interface/properties :heraldry.charge.type/other [{:keys [load-charge-data] :as context}]
  (let [variant (interface/get-raw-data (c/++ context :variant))
        {charge-entity :entity
         entity-path :path} (when variant (load-charge-data variant))
        {:keys [width
                height
                data
                shapes]} (-> charge-entity :data :edn-data)
        base-width (js/parseFloat (or width "1"))
        base-height (js/parseFloat (or height "1"))
        base-shape (if (seq shapes)
                     (mapv (fn [path]
                             (-> path
                                 first
                                 path/parse-path
                                 (path/translate (v/Vector. (- (/ base-width 2)) (- (/ base-height 2))))
                                 path/to-svg))
                           shapes)
                     [["M" (- (/ base-width 2)) (- (/ base-height 2))
                       "h" base-width
                       "v" base-height
                       "h" (- base-width)
                       "z"]])
        unadjusted-charge (clean-charge-data data)]
    (charge.shared/process-shape
     context
     {:base-shape base-shape
      :base-width base-width
      :base-height base-height
      :other? true
      :charge-entity charge-entity
      :charge-entity-path entity-path
      :unadjusted-charge unadjusted-charge})))

(defn- picked-replacements
  "The colours the tinctures of a charge resolve to, by placeholder kind. As
  data, they can key the memoized recolourings below, which a closure over the
  context couldn't."
  [tincture context]
  (into {}
        (keep (fn [[kind replacement]]
                (when (and (keyword? replacement)
                           (not= replacement :none))
                  [kind (tincture/pick replacement context)])))
        tincture))

;; drawn on top of the masked field: replaced and kept colours, everything else
;; in the outline colour, see the comment in render-ir
(def ^:private colour-charge
  (cache/memoize
   ::colour-charge
   (fn colour-charge [data placeholder-colours picks outline-colour]
     (replace-colours
      data
      (fn [colour]
        (let [kind (placeholder-colour-modifier placeholder-colours (colour/normalize colour))]
          (or (get picks kind)
              (if (= kind :keep)
                colour
                outline-colour))))))))

;; the colours the masks would show: the field's colour where the field shows
;; through, the tinctures and kept colours where the coloured charge shows, and
;; the outline colour where only the base layer shows
(def ^:private colour-charge-directly
  (cache/memoize
   ::colour-charge-directly
   (fn colour-charge-directly [data placeholder-colours picks outline-colour outline-mode field-colour]
     (replace-colours
      data
      (fn [colour]
        (if (str/starts-with? colour "url")
          "none"
          (let [kind (placeholder-colour-modifier placeholder-colours (colour/normalize colour))]
            (cond
              (get picks kind) (get picks kind)
              (= kind :keep) colour
              (= kind :layer-separator) "none"
              (and (= kind :outline)
                   (not= outline-mode :primary)) outline-colour
              :else field-colour))))))))

(defn- plain-field-colour
  "The flat colour of a plain field without components, or nil if the field
  needs to be rendered, e.g. for a fur, a hatching or a division."
  [field-context]
  (when (and (= (interface/get-sanitized-data (c/++ field-context :type))
                :heraldry.field.type/plain)
             (zero? (interface/get-list-size (c/++ field-context :components))))
    (let [colour (tincture/pick (interface/get-sanitized-data (c/++ field-context :tincture))
                                field-context)]
      (when-not (str/starts-with? colour "url")
        colour))))

(defn render-ir [context {:keys [anchor-point scale-x scale-y angle top-left unadjusted-charge
                                 charge-entity charge-entity-path]}]
  (let [{:keys [self-below-shield?
                render-pass-below-shield?
                ui-show-colours
                preview-original?
                charge-preview?
                svg-export?
                clip?
                select-component-fn
                select-field-fn
                enter-component-fn
                leave-component-fn]} (c/render-hints context)
        charge-data (-> charge-entity :data :edn-data)
        charge-shapes (:shapes charge-data)
        base-width (js/parseFloat (-> charge-data :width (or "1")))
        base-height (js/parseFloat (-> charge-data :height (or "1")))
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
            fixed-tincture (-> charge-entity :data :fixed-tincture (or :none))
            render-field? (= fixed-tincture :none)
            landscape? (-> charge-entity :data :landscape?)
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
            ;; on a flat-coloured field the charge can be painted directly instead of
            ;; showing the field through masks; exports only, because in the editor the
            ;; field inside the charge is a click target of its own; a layer separator
            ;; that hides lower layers needs a mask to cut them away
            field-colour (when (and svg-export?
                                    render-field?
                                    (not (or preview-original?
                                             landscape?
                                             highlight-colours?
                                             hide-lower-layer?)))
                           (plain-field-colour (c/++ context :field)))
            {mask :mask
             mask-inverted :mask-inverted
             mask-base :mask-base} (when-not (or preview-original?
                                                 landscape?
                                                 field-colour)
                                     (mask-defs adjusted-charge-without-shading
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
            picks (picked-replacements tincture context)
            outline-colour (outline/color context)
            coloured-charge (when-not field-colour
                              (colour-charge adjusted-charge-without-shading
                                             placeholder-colours
                                             picks
                                             outline-colour))
            directly-coloured-charge (when field-colour
                                       (colour-charge-directly adjusted-charge-without-shading
                                                               placeholder-colours
                                                               picks
                                                               outline-colour
                                                               outline-mode
                                                               field-colour))
            layer-separator-colour-for-shadow-highlight (if hide-lower-layer?
                                                          "#000000"
                                                          "none")
            shadow-mask (when render-shadow?
                          (shading-mask-def :shadow
                                            adjusted-charge-without-shading
                                            adjusted-charge
                                            placeholder-colours
                                            layer-separator-colour-for-shadow-highlight))
            highlight-mask (when render-highlight?
                             (shading-mask-def :highlight
                                               adjusted-charge-without-shading
                                               adjusted-charge
                                               placeholder-colours
                                               layer-separator-colour-for-shadow-highlight))
            transform (str "translate(" (v/->str anchor-point) ")"
                           "rotate(" angle ")"
                           "scale(" scale-x "," scale-y ")"
                           "translate(" (v/->str top-left) ")")
            reverse-transform (str "translate(" (-> top-left (v/mul -1) v/->str) ")"
                                   "scale(" (/ 1 scale-x) "," (/ 1 scale-y) ")"
                                   "rotate(" (- angle) ")"
                                   "translate(" (-> anchor-point (v/mul -1) v/->str) ")")
            shading-layers [:<>
                            (when render-shadow?
                              [:g {:mask (str "url(#" (:id shadow-mask) ")")}
                               [:rect {:transform reverse-transform
                                       :x -500
                                       :y -500
                                       :width 1100
                                       :height 1100
                                       :fill "#001040"
                                       :style {:opacity (:shadow tincture)}}]])
                            (when render-highlight?
                              [:g {:mask (str "url(#" (:id highlight-mask) ")")}
                               [:rect {:transform reverse-transform
                                       :x -500
                                       :y -500
                                       :width 1100
                                       :height 1100
                                       :fill "#ffffe8"
                                       :style {:opacity (:highlight tincture)}}]])]
            charge-clip-path (when (or clip?
                                       (not (or svg-export?
                                                preview-original?)))
                               (clip-path-def charge-shapes base-width base-height))]
        (-> ir/blank
            (cond->
              shadow-mask (ir/add-def [:mask {:id (:id shadow-mask)}
                                       (:content shadow-mask)])
              highlight-mask (ir/add-def [:mask {:id (:id highlight-mask)}
                                          (:content highlight-mask)])
              mask (ir/add-def [:mask {:id (:id mask)}
                                (:content mask)])
              mask (ir/add-def [:mask {:id (:id mask-inverted)}
                                (:content mask-inverted)])
              (and mask
                   (= outline-mode :keep)) (ir/add-def [:mask {:id (:id mask-base)}
                                                        (:content mask-base)])
              charge-clip-path (ir/add-def [:clipPath {:id (:id charge-clip-path)}
                                            (:content charge-clip-path)])
              (and render-field?
                   (not (or preview-original?
                            landscape?
                            highlight-colours?
                            field-colour))) (ir/add-child (c/++ context :field)))
            (assoc
             :body
             [:<>
              [:g (when-not (or svg-export?
                                charge-preview?)
                    {:on-click (when select-component-fn
                                 (js-event/handled
                                  #(select-component-fn context)))
                     :on-double-click (when (and (not svg-export?)
                                                 select-field-fn)
                                        (js-event/handled
                                         #(select-field-fn context)))
                     :on-mouse-enter (when enter-component-fn
                                       (js-event/handled
                                        #(enter-component-fn context)))
                     :on-mouse-leave (when enter-component-fn
                                       (js-event/handled
                                        #(leave-component-fn context)))
                     :style {:cursor "pointer"}})
               [:g {:transform transform}
                [render/shape-fimbriation context
                 :fimbriation-shape fimbriation-shape
                 :reverse-transform reverse-transform
                 :scale (Math/abs (/ 1 scale-x))]
                [svg.metadata/attribution {:path charge-entity-path}]
                [:g (when charge-clip-path
                      {:clip-path (str "url(#" (:id charge-clip-path) ")")})
                 (cond
                   preview-original? (cond-> (svg/make-unique-ids unadjusted-charge)
                                       highlight-colours? (replace-colours
                                                           (fn [colour]
                                                             (highlight-colour
                                                              colour ui-show-colours))))
                   landscape? unadjusted-charge
                   highlight-colours? adjusted-charge
                   directly-coloured-charge [:<>
                                             (svg/make-unique-ids directly-coloured-charge)

                                             shading-layers]
                   :else [:<>
                          (when (= outline-mode :keep)
                            [:g {:mask (str "url(#" (:id mask-base) ")")}
                             [:rect {:transform reverse-transform
                                     :x -500
                                     :y -500
                                     :width 1100
                                     :height 1100
                                     :fill (outline/color context)}]])

                          (when render-field?
                            [:g {:mask (str "url(#" (:id mask-inverted) ")")}
                             [:g {:transform reverse-transform}
                              [interface/ir-node (c/++ context :field)]]])

                          [:g {:mask (str "url(#" (:id mask) ")")}
                           (svg/make-unique-ids coloured-charge)]

                          shading-layers])]]]

              (when (and (not svg-export?)
                         @(rf/subscribe [::tree/node-highlighted?
                                         ::arms.details/identifier
                                         (conj (:path context) :field)]))
                [:g {:transform transform}
                 (for [path charge-shapes]
                   ^{:key path}
                   [:path.node-highlighted {:d (str/join " " path)
                                            :style {:stroke-width (/ 1 scale-x)
                                                    :stroke-linecap "round"
                                                    :stroke-linejoin "round"
                                                    :fill "none"
                                                    :pointer-events "none"}}])])])))
      ir/blank)))
