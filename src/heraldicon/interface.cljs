(ns heraldicon.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.math.bounding-box :as bb]
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

(defn parent [context]
  (some-> (cond
            ;; TODO: can all this be done without inspecting the path?
            (-> context :path last (= :coat-of-arms)) nil
            (-> context :path last (= :field)) (c/-- context)
            (-> context :path drop-last last (= :charges)) (c/-- context 4)
            (-> context :path last int?) (c/-- context 2)
            (-> context :path drop-last last (= :cottising)) (parent (c/-- context 2))
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

(defmulti environment effective-component-type)

(defmethod environment :default [_context])

(defmulti subfield-environments effective-component-type)

(defmethod subfield-environments nil [_context])
(defmethod subfield-environments :default [context]
  (log/warn :not-implemented 'subfield-environments context))

(rf/reg-sub-raw ::subfield-environments
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::subfield-environments
     context
     #(let [context (resolve-context context)]
        (subfield-environments context)))))

(defn get-subfield-environments [context]
  @(rf/subscribe [::subfield-environments (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::environment
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::environment
     context
     #(let [context (resolve-context context)]
        (environment context)))))

(rf/reg-sub-raw ::parent-environment
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::parent-environment
     context
     #(or (c/get-key context :parent-environment-override)
          @(rf/subscribe [::environment (parent context)])))))

(defn get-parent-environment [context]
  @(rf/subscribe [::parent-environment (c/scrub-render-hints context)]))

(defn get-environment [context]
  @(rf/subscribe [::environment (c/scrub-render-hints context)]))

(defmulti render-shape effective-component-type)

(defmethod render-shape nil [context]
  (log/warn :not-implemented 'render-shape context))

(defmulti subfield-render-shapes effective-component-type)

(defmethod subfield-render-shapes nil [_context])
(defmethod subfield-render-shapes :default [context]
  (log/warn :not-implemented 'subfield-render-shapes context))

(rf/reg-sub-raw ::subfield-render-shapes
  (fn [_app-db [_ context]]
    (reaction-or-cache
     :subfield-render-shapes
     context
     #(let [context (resolve-context context)]
        (subfield-render-shapes context)))))

(defn get-subfield-render-shapes [context]
  @(rf/subscribe [::subfield-render-shapes (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::render-shape
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::render-shape
     context
     #(let [context (resolve-context context)]
        (render-shape context)))))

(defn get-render-shape [context]
  @(rf/subscribe [::render-shape (c/scrub-render-hints context)]))

(defmulti exact-shape effective-component-type)

(defn get-exact-shape [context]
  @(rf/subscribe [::exact-shape (c/scrub-render-hints context)]))

(defn get-exact-parent-shape [context]
  (or (c/get-key context :parent-shape)
      @(rf/subscribe [::exact-shape (c/scrub-render-hints (parent context))])))

(rf/reg-sub-raw ::exact-impacted-shape
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::exact-impacted-shape
     context
     (fn []
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
                                 :shape
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
           base-shape (environment/subtract-shape base-shape)))))))

(defn get-exact-impacted-shape [context]
  @(rf/subscribe [::exact-impacted-shape (c/scrub-render-hints context)]))

(declare get-parent-field-shape)

(defn fallback-exact-shape [context]
  (let [shape-path (:shape (get-render-shape context))
        shape-path (if (vector? shape-path)
                     (first shape-path)
                     shape-path)]
    (environment/intersect-shapes
     shape-path
     (get-parent-field-shape context))))

(defmethod exact-shape :default [context]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/subfield [context]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/ordinary [context]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/charge [context]
  ;; the charge dictates its own field, the parent field's shape does
  ;; not affect it like it does for ordinaries
  (-> (get-render-shape context) :shape first))

(rf/reg-sub-raw ::exact-shape
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::exact-shape
     context
     #(let [context (resolve-context context)]
        (exact-shape context)))))

(rf/reg-sub-raw ::field-edges
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::field-edges
     context
     #(:edges (subfield-render-shapes context)))))

(defn get-field-edges [context]
  @(rf/subscribe [::field-edges (c/scrub-render-hints context)]))

(defmulti bounding-box effective-component-type)

(defmethod bounding-box :default [context]
  (:bounding-box (get-environment context)))

(rf/reg-sub-raw ::bounding-box
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::bounding-box
     context
     #(let [context (resolve-context context)]
        (bounding-box context)))))

(defn get-bounding-box [context]
  @(rf/subscribe [::bounding-box (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::impacted-environment
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::impacted-environment
     context
     (fn []
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
           (environment/create new-bounding-box nil :root? root?)))))))

(defn get-impacted-environment [context]
  @(rf/subscribe [::impacted-environment (c/scrub-render-hints context)]))

(defmulti auto-ordinary-info (fn [ordinary-type _context]
                               ordinary-type))

(rf/reg-sub-raw ::auto-ordinary-info
  (fn [_app-db [_ ordinary-type context]]
    (reaction-or-cache
     [::auto-ordinary-info ordinary-type]
     context
     #(auto-ordinary-info ordinary-type context))))

(defn get-auto-ordinary-info [ordinary-type context]
  @(rf/subscribe [::auto-ordinary-info ordinary-type (c/scrub-render-hints context)]))

(defmulti auto-arrangement (fn [ordinary-type _context]
                             ordinary-type))

(defmethod auto-arrangement :default [ordinary-type context]
  (log/warn :not-implemented "interface.auto-arrangement" ordinary-type context))

(rf/reg-sub-raw ::auto-arrangement
  (fn [_app-db [_ ordinary-type context]]
    (reaction-or-cache
     [::auto-arrangement ordinary-type]
     context
     #(auto-arrangement ordinary-type context))))

(defn get-auto-arrangement [ordinary-type context]
  @(rf/subscribe [::auto-arrangement ordinary-type (c/scrub-render-hints context)]))

(defmulti parent-field-environment effective-component-type)

(defmethod parent-field-environment :default [context]
  (let [parent-context (parent context)]
    (if (get-sanitized-data (c/++ context :adapt-to-ordinaries?))
      (get-impacted-environment parent-context)
      (get-environment parent-context))))

(rf/reg-sub-raw ::parent-field-environment
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::parent-field-environment
     context
     #(or (c/get-key context :parent-environment-override)
          (parent-field-environment context)))))

(defn get-parent-field-environment [context]
  @(rf/subscribe [::parent-field-environment (c/scrub-render-hints context)]))

(defmulti parent-field-shape effective-component-type)

(defmethod parent-field-shape :default [context]
  (let [parent-context (parent context)]
    (if (get-sanitized-data (c/++ context :adapt-to-ordinaries?))
      (get-exact-impacted-shape parent-context)
      (get-exact-shape parent-context))))

(rf/reg-sub-raw ::parent-field-shape
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::parent-field-shape
     context
     #(parent-field-shape context))))

(defn get-parent-field-shape [context]
  @(rf/subscribe [::parent-field-shape (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::subfields-environment
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::subfields-environment
     context
     #(if (get-sanitized-data (c/++ context :adapt-to-ordinaries?))
        (get-impacted-environment context)
        (get-environment context)))))

(defn get-subfields-environment [context]
  @(rf/subscribe [::subfields-environment (c/scrub-render-hints context)]))

(rf/reg-sub-raw ::subfields-shape
  (fn [_app-db [_ context]]
    (reaction-or-cache
     ::subfields-shape
     context
     #(if (get-sanitized-data (c/++ context :adapt-to-ordinaries?))
        (get-exact-impacted-shape context)
        (get-exact-shape context)))))

(defn get-subfields-shape [context]
  @(rf/subscribe [::subfields-shape (c/scrub-render-hints context)]))
