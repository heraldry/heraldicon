(ns heraldicon.interface
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.counterchange :as counterchange]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.options :as options]
   [heraldicon.render.ir :as ir]
   [heraldicon.render.options :as render.options]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros
   [heraldicon.interface.macros :refer [defattribute]]
   [reagent.ratom :refer [reaction]]))

(def ^:dynamic cache
  (atom {}))

(defn clear-subscription-cache! []
  (reset! cache {}))

(defn reaction-or-cache [id
                         {:keys [cache-subscriptions?]
                          :as context} f]
  (if cache-subscriptions?
    (let [key [id context]
          value (or (get @cache key) (f))]
      (swap! cache assoc key value)
      (reaction value))
    (reaction (f))))

(defn get-raw-data [{:keys [path]}]
  @(rf/subscribe [:get path]))

(defn effective-component-type [context]
  (let [component-type (component/effective-type (get-raw-data (c/++ context :type)))]
    (if (some-> component-type namespace (= "heraldry.charge.type"))
      (let [data (get-raw-data (c/++ context :data))
            variant (get-raw-data (c/++ context :variant))]
        ;; TODO: this would fail if there's ever a charge-type for which no render method
        ;; exists and no variant is given
        (if (or data
                (seq variant)
                (= component-type :heraldry.charge.type/preview))
          :heraldry.charge.type/other
          component-type))
      component-type)))

(defmulti options effective-component-type)

(defmethod options :default [_context]
  nil)

(rf/reg-sub ::sanitized-data
  (fn [[_ {:keys [path]
           :as context}] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [::options context])])

  (fn [[data options] [_ _context]]
    (options/sanitize-value-or-data data options)))

(defn get-sanitized-data [context]
  @(rf/subscribe [::sanitized-data (c/scrub-render-hints context)]))

(defn get-list-size [{:keys [path]}]
  @(rf/subscribe [:get-list-size path]))

(defattribute get-options [context]
  (when (seq (:path context))
    (or (options context)
        (get (get-options (c/-- context)) (-> context :path last)))))

(defn render-option [key {:keys [render-options-path
                                 override-render-options-path] :as context}]
  (let [override-value (when override-render-options-path
                         (let [value @(rf/subscribe [:get (conj override-render-options-path key)])]
                           ;; treat :none as nil
                           (when (not= value :none)
                             value)))]
    (if (some? override-value)
      override-value
      (if render-options-path
        (get-sanitized-data (c/<< context :path (conj render-options-path key)))
        (get (options/sanitize-value-or-data default/render-options (render.options/build nil)) key)))))

(defmulti render-component effective-component-type)

(defmethod render-component nil [context]
  (log/warn :not-implemented "render-component" context)
  [:<>])

;; Render results as data, see heraldicon.render.ir. A component type that
;; implements render-ir renders its children as ir-nodes and lists them in
;; :children; its render-component is render-ir-root, which emits the defs of
;; the whole subtree once. Types without render-ir are wrapped as they are.
(defmulti render-ir effective-component-type)

(defmethod render-ir :default [context]
  (assoc ir/blank :body [render-component context]))

(rf/reg-sub-raw ::render-ir
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::render-ir
     context
     #(render-ir context))))

(rf/reg-sub-raw ::render-defs
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::render-defs
     context
     #(let [{:keys [defs children]} @(rf/subscribe [::render-ir context])]
        (transduce (map (fn [child-context]
                          @(rf/subscribe [::render-defs child-context])))
                   merge
                   defs
                   children)))))

(defn ir-node
  "Renders the body of a node; its defs are emitted by the nearest
  render-ir-root above it."
  [context]
  (:body @(rf/subscribe [::render-ir context])))

(defn render-ir-root [context]
  [:<>
   (ir/defs->hiccup @(rf/subscribe [::render-defs context]))
   [ir-node context]])

(defmulti blazon-component effective-component-type)

(defmethod blazon-component nil [context]
  (log/warn "blazon: unknown component" context)
  nil)

(defn blazon [context]
  (let [manual-blazon (get-sanitized-data (c/++ context :manual-blazon))]
    (if (str/blank? manual-blazon)
      (blazon-component context)
      (str "[" manual-blazon "]"))))

(declare parent)

(defn parent-node-context
  "The node that contains the component in the data tree. The position in the
  data decides it, not the component type:

  - a :field belongs to the component that owns it
  - an element of a list such as :components, :fields, :elements or :charges
    belongs to the owner of the list
  - a cottise belongs to the field of its ordinary

  Anything else, e.g. the coat of arms itself, has no parent."
  [{:keys [path]
    :as context}]
  (let [[container slot] (take-last 2 path)]
    (some-> (cond
              (= slot :coat-of-arms) nil
              (= slot :field) (c/-- context)
              (int? slot) (c/-- context 2)
              (= container :cottising) (parent (c/-- context 2))
              :else nil)
            c/remove-keys-for-children)))

(defn parent
  "The component whose geometry this one depends on. That's the node
  parent, except for charges in a charge group, which depend on the field the
  group sits in."
  [{:keys [path]
    :as context}]
  (let [node-parent (parent-node-context context)]
    (if (= (-> path drop-last last) :charges)
      (parent-node-context node-parent)
      node-parent)))

(defn- resolve-context [context]
  (if-let [path-redirect (c/get-key context :path-redirect)]
    ;; TODO: clean up old path's component data?
    (c/<< context :path path-redirect)
    context))

(defmulti properties effective-component-type)

(defmethod properties :default [context]
  (when context
    (log/warn :not-implemented "interface.properties" context)))

(defattribute get-properties [context]
  (properties context))

(defmulti environment effective-component-type)

(defmethod environment :default [_context])

(defmulti subfield-environments effective-component-type)

(defmethod subfield-environments nil [_context])
(defmethod subfield-environments :default [context]
  (log/warn :not-implemented 'subfield-environments context))

(declare get-subfields-environment)

(defattribute get-subfield-environments [context]
  (let [context (resolve-context context)
        result (subfield-environments context)
        parent-bounding-box (:bounding-box (get-subfields-environment context))]
    (cond-> result
      (and parent-bounding-box
           (:subfields result))
      (update :subfields
              (fn [subfields]
                (mapv (fn [subfield-environment]
                        (environment/clamp-to-bounding-box subfield-environment parent-bounding-box))
                      subfields))))))

(defattribute get-environment [context]
  (environment (resolve-context context)))

(defattribute get-parent-environment [context]
  (or (c/get-key context :parent-environment-override)
      (get-environment (parent context))))

(defmulti render-shape effective-component-type)

(defmethod render-shape nil [context]
  (log/warn :not-implemented 'render-shape context))

(defmulti subfield-render-shapes effective-component-type)

(defmethod subfield-render-shapes nil [_context])
(defmethod subfield-render-shapes :default [context]
  (log/warn :not-implemented 'subfield-render-shapes context))

(defattribute get-subfield-render-shapes [context]
  (subfield-render-shapes (resolve-context context)))

(defattribute get-render-shape [context]
  (render-shape (resolve-context context)))

(defmulti exact-shape effective-component-type)

(declare get-exact-shape)

(defn get-exact-parent-shape [context]
  (or (c/get-key context :parent-shape)
      (get-exact-shape (parent context))))

(defn geometry-shape
  "The path(s) a render-shape exposes for geometric computation. Prefers the
  clean (non-squigglified) :clean-shape when a producer provides one, so that
  exact-shape and everything downstream of it (intersections, shrinking,
  couped/humetty, divisions) never run on the inflated squiggly display path.
  Falls back to :shape for producers that don't (yet) distinguish the two."
  [render-shape]
  (or (:clean-shape render-shape)
      (:shape render-shape)))

(defattribute get-exact-impacted-shape [context]
  (let [context (resolve-context context)
        shape (exact-shape context)
        components-context (c/++ context :components)
        num-components (get-list-size components-context)
        impactful-ordinary-contexts (into []
                                          (comp
                                           (map #(c/++ components-context %))
                                           (filter #(#{:heraldry.ordinary.type/chief
                                                       :heraldry.ordinary.type/base}
                                                     (get-raw-data (c/++ % :type)))))
                                          (range num-components))
        component-properties (map (fn [context]
                                    [context (get-properties context)])
                                  impactful-ordinary-contexts)
        chief-context (->> component-properties
                           (filter (comp #{:heraldry.ordinary.type/chief} :type second))
                           (sort-by (comp :y first :lower second) >)
                           first
                           first)
        chief-shape (some-> chief-context
                            get-render-shape
                            geometry-shape
                            first)
        base-context (->> component-properties
                          (filter (comp #{:heraldry.ordinary.type/base} :type second))
                          (sort-by (comp :y first :upper second) <)
                          first
                          first)
        base-shape (some-> base-context
                           get-render-shape
                           :shape
                           first)]
    (cond-> shape
      chief-shape (environment/subtract-shape chief-shape)
      base-shape (environment/subtract-shape base-shape))))

(declare get-parent-field-shape)

(defn fallback-exact-shape [context]
  (let [shape-path (geometry-shape (get-render-shape context))
        shape-path (if (vector? shape-path)
                     (first shape-path)
                     shape-path)]
    (environment/intersect-shapes
     shape-path
     (get-parent-field-shape context))))

(defmethod exact-shape :default [context]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/ordinary [context]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/charge [context]
  ;; the charge dictates its own field, the parent field's shape does
  ;; not affect it like it does for ordinaries
  (-> (get-render-shape context) geometry-shape first))

(defattribute get-exact-shape [context]
  (exact-shape (resolve-context context)))

(defattribute get-field-edges [context]
  (:edges (subfield-render-shapes context)))

(defmulti bounding-box effective-component-type)

(defmethod bounding-box :default [context]
  (:bounding-box (get-environment context)))

(defattribute get-bounding-box [context]
  (bounding-box (resolve-context context)))

(defattribute get-impacted-environment [context]
  (let [{:keys [bounding-box
                root?]
         :as environment} (get-environment context)
        components-context (c/++ context :components)
        num-components (get-list-size components-context)
        impactful-ordinary-contexts (into []
                                          (comp
                                           (map #(c/++ components-context %))
                                           (filter #(#{:heraldry.ordinary.type/chief
                                                       :heraldry.ordinary.type/base}
                                                     (get-raw-data (c/++ % :type)))))
                                          (range num-components))
        component-properties (map get-properties impactful-ordinary-contexts)
        chief-impact (->> component-properties
                          (filter (comp #{:heraldry.ordinary.type/chief} :type))
                          (map (comp :y first :lower))
                          (apply max))
        base-impact (->> component-properties
                         (filter (comp #{:heraldry.ordinary.type/base} :type))
                         (map (comp :y first :upper))
                         (apply min))
        new-bounding-box (cond-> bounding-box
                           chief-impact (bb/shrink-top chief-impact :min-height 5)
                           base-impact (bb/shrink-bottom base-impact :min-height 5))]
    (if (= new-bounding-box bounding-box)
      environment
      (environment/create new-bounding-box nil :root? root?))))

(defmulti auto-ordinary-info (fn [ordinary-type _context]
                               ordinary-type))

(defattribute get-auto-ordinary-info [ordinary-type context]
  (auto-ordinary-info ordinary-type context))

(defmulti auto-arrangement (fn [ordinary-type _context]
                             ordinary-type))

(defmethod auto-arrangement :default [ordinary-type context]
  (log/warn :not-implemented "interface.auto-arrangement" ordinary-type context))

(defattribute get-auto-arrangement [ordinary-type context]
  (auto-arrangement ordinary-type context))

(defmulti parent-field-environment effective-component-type)

(defmethod parent-field-environment :default [context]
  (let [parent-context (parent context)
        context-for-adapt-check (if (isa? (effective-component-type context) :heraldry/subfield)
                                  parent-context
                                  context)]
    (if (get-sanitized-data (c/++ context-for-adapt-check :adapt-to-ordinaries?))
      (get-impacted-environment parent-context)
      (get-environment parent-context))))

(defattribute get-parent-field-environment [context]
  (or (c/get-key context :parent-environment-override)
      (parent-field-environment context)))

(defmulti parent-field-shape effective-component-type)

(defmethod parent-field-shape :default [context]
  (let [parent-context (parent context)
        context-for-adapt-check (if (isa? (effective-component-type context) :heraldry/subfield)
                                  parent-context
                                  context)]
    (if (get-sanitized-data (c/++ context-for-adapt-check :adapt-to-ordinaries?))
      (get-exact-impacted-shape parent-context)
      (get-exact-shape parent-context))))

(defattribute get-parent-field-shape [context]
  (parent-field-shape context))

(defattribute get-subfields-environment [context]
  (if (get-sanitized-data (c/++ context :adapt-to-ordinaries?))
    (get-impacted-environment context)
    (get-environment context)))

(defattribute get-subfields-shape [context]
  (if (get-sanitized-data (c/++ context :adapt-to-ordinaries?))
    (get-exact-impacted-shape context)
    (get-exact-shape context)))

(defn counterchanged-tinctures [context]
  @(rf/subscribe [::counterchanged-tinctures context]))

(rf/reg-sub-raw ::counterchanged-tinctures
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::counterchanged-tinctures
     context
     #(if (= (get-sanitized-data (c/++ context :type))
             :heraldry.field.type/counterchanged)
        (-> (counterchanged-tinctures (parent (parent context)))
            reverse
            vec)
        (counterchange/get-tinctures @(rf/subscribe [:get context]) context)))))
