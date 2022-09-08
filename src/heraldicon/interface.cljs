(ns heraldicon.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.environment :as field.environment]
   [heraldicon.options :as options]
   [heraldicon.render.options :as render.options]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private cache
  (atom {}))

(defn clear-subscription-cache! []
  (reset! cache {}))

(defn- reaction-or-cache [id
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

(defmulti options (fn [{:keys [dispatch-value] :as context}]
                    (or dispatch-value
                        (effective-component-type context))))

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
  @(rf/subscribe [::sanitized-data context]))

(defn get-list-size [{:keys [path]}]
  @(rf/subscribe [:get-list-size path]))

(defn get-options [context]
  @(rf/subscribe [::options (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::options
  (fn [_app-db [_ {:keys [path]
                   :as context}]]
    (reaction-or-cache
     ::options
     context
     #(when (seq path)
        (or (options context)
            (get (get-options (c/-- context)) (last path)))))))

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

(defmulti blazon-component effective-component-type)

(defmethod blazon-component nil [context]
  (log/warn "blazon: unknown component" context)
  nil)

(defn blazon [context]
  (let [manual-blazon (get-sanitized-data (c/++ context :manual-blazon))]
    (if (-> manual-blazon count pos?)
      manual-blazon
      (blazon-component context))))

(defn- cottise-context? [context]
  (-> context :path drop-last last (= :cottising)))

(defn parent [context]
  (some-> (cond
            (-> context :path last (= :field)) (c/-- context)
            (-> context :path drop-last last (= :charges)) (c/-- context 4)
            (-> context :path last int?) (c/-- context 2)
            (cottise-context? context) (c/-- context 2)
            :else (do
                    (log/warn :not-implemented "parent" context)
                    nil))
          c/remove-keys-for-children))

(defn- resolve-context [context]
  (if-let [path-redirect (c/get-key context :path-redirect)]
    ;; TODO: clean up old path's component data?
    (c/<< context :path path-redirect)
    context))

(defmulti properties effective-component-type)

(defmethod properties :default [context]
  (log/warn :not-implemented "interface.properties" context))

(rf/reg-sub-raw ::properties
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::properties
     context
     #(properties context))))

(defn get-properties [context]
  @(rf/subscribe [::properties (c/scrub-render-hints context)]))

(defmulti environment (fn [_context properties]
                        (:type properties)))

(defmethod environment :default [_context _properties])

(defmulti subfield-environments (fn [_context properties]
                                  (:type properties)))

(defmethod subfield-environments nil [_context _properties])
(defmethod subfield-environments :default [context _properties]
  (log/warn :not-implemented 'subfield-environments context))

(rf/reg-sub-raw ::subfield-environments
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::subfield-environments
     context
     #(let [context (resolve-context context)]
        (subfield-environments context (get-properties context))))))

(defn get-subfield-environments [context]
  @(rf/subscribe [::subfield-environments (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::environment
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::environment
     context
     #(let [context (resolve-context context)]
        (environment context (get-properties context))))))

(defn get-parent-environment [context]
  (or (c/get-key context :parent-environment-override)
      (if (cottise-context? context)
        @(rf/subscribe [::environment (c/scrub-render-hints (parent (parent context)))])
        @(rf/subscribe [::environment (c/scrub-render-hints (parent context))]))))

(defn get-environment [context]
  @(rf/subscribe [::environment (c/scrub-render-hints context)]))

(defmulti render-shape (fn [_context properties]
                         (:type properties)))

(defmethod render-shape nil [_context _properties])

(defmulti subfield-render-shapes (fn [_context properties]
                                   (:type properties)))

(defmethod subfield-render-shapes nil [_context _properties])
(defmethod subfield-render-shapes :default [context _properties]
  (log/warn :not-implemented 'subfield-render-shapes context))

(rf/reg-sub-raw ::subfield-render-shapes
  (fn [_app-db [_ context]]
    (reaction-or-cache
     :subfield-render-shapes
     context
     #(let [context (resolve-context context)]
        (subfield-render-shapes context (get-properties context))))))

(defn get-subfield-render-shapes [context]
  @(rf/subscribe [::subfield-render-shapes (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::render-shape
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::render-shape
     context
     #(let [context (resolve-context context)]
        (render-shape context (get-properties context))))))

(defn get-render-shape [context]
  @(rf/subscribe [::render-shape (c/scrub-render-hints context)]))

(defmulti exact-shape (fn [_context properties]
                        (:type properties)))

(defn get-exact-shape [context]
  @(rf/subscribe [::exact-shape (c/scrub-render-hints context)]))

(defn get-exact-parent-shape [context]
  (or (c/get-key context :parent-shape)
      (if (cottise-context? context)
        @(rf/subscribe [::exact-shape (c/scrub-render-hints (parent (parent context)))])
        @(rf/subscribe [::exact-shape (c/scrub-render-hints (parent context))]))))

(defn fallback-exact-shape [context]
  (let [shape-path (:shape (get-render-shape context))
        shape-path (if (vector? shape-path)
                     (first shape-path)
                     shape-path)]
    (field.environment/intersect-shapes
     shape-path
     (get-exact-shape (parent context)))))

(defmethod exact-shape nil [context _properties]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/subfield [context _properties]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/ordinary [context _properties]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/charge [context _properties]
  ;; the charge dictates its own field, the parent field's shape does
  ;; not affect it like it does for ordinaries
  (-> (get-render-shape context) :shape first))

(rf/reg-sub-raw ::exact-shape
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::exact-shape
     context
     #(let [context (resolve-context context)]
        (exact-shape context (get-properties context))))))

(rf/reg-sub-raw ::field-edges
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::field-edges
     context
     #(:edges (subfield-render-shapes context (get-properties context))))))

(defn get-field-edges [context]
  @(rf/subscribe [::field-edges (c/scrub-render-hints context)]))

(defmulti bounding-box (fn [_context properties]
                         (:type properties)))

(defmethod bounding-box :default [context _properties]
  (:bounding-box (get-environment context)))

(rf/reg-sub-raw ::bounding-box
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::bounding-box
     context
     #(let [context (resolve-context context)]
        (bounding-box context (get-properties context))))))

(defn get-bounding-box [context]
  @(rf/subscribe [::bounding-box (c/scrub-render-hints context)]))

(defn get-effective-parent-environment [context]
  (if (get-sanitized-data (c/++ context :inherit-environment?))
    (get-parent-environment (parent context))
    (get-parent-environment context)))

(defn get-effective-parent-shape [context]
  (if (get-sanitized-data (c/++ context :inherit-environment?))
    (get-exact-shape (parent (parent context)))
    (get-exact-shape (parent context))))

(defn get-counterchange-parent [context]
  (if (cottise-context? context)
    (parent (parent context))
    (parent context)))
