(ns heraldicon.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.field.environment :as field.environment]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defn get-raw-data [{:keys [path subscriptions] :as context}]
  (cond
    subscriptions (let [{:keys [base-path data]} subscriptions
                        relative-path (-> base-path count (drop path) vec)]
                    (if (contains? data relative-path)
                      (get data relative-path)
                      (do
                        (log/error (str "Missing subscription: " path " context: " context))
                        (get-raw-data (dissoc context :subscriptions)))))
    (-> path first (= :context)) (get-in context (drop 1 path))
    :else @(rf/subscribe [:get path])))

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

(defmethod options nil [_context]
  nil)

(defmulti options-subscriptions (fn [{:keys [dispatch-value] :as context}]
                                  (or dispatch-value
                                      (effective-component-type context))))

(defmethod options-subscriptions nil [_context]
  nil)

;; TODO: this is one of the biggest potential bottle necks
(defn get-relevant-options [{:keys [path] :as context}]
  (if (-> path first (not= :context))
    @(rf/subscribe [::options (:path context)])
    (let [[options relative-path] (or (->> (range (count path) 0 -1)
                                           (keep (fn [idx]
                                                   (let [option-path (subvec path 0 idx)
                                                         relative-path (subvec path idx)
                                                         options (options
                                                                  (c/<< context :path option-path))]
                                                     (when options
                                                       [options relative-path]))))
                                           first)
                                      [nil nil])]
      (get-in options relative-path))))

(rf/reg-sub ::sanitized-data
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [::options path])])

  (fn [[data options] [_ _path]]
    (options/sanitize-value-or-data data options)))

(defn get-sanitized-data [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (let [data (get-raw-data context)
          options (get-relevant-options context)]
      (options/sanitize-value-or-data data options))
    @(rf/subscribe [::sanitized-data (:path context)])))

(defn get-list-size [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (count (get-in context (drop 1 path)))
    @(rf/subscribe [:get-list-size path])))

(rf/reg-sub ::component-type
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :type)]))

  (fn [raw-type _]
    (component/effective-type raw-type)))

(rf/reg-sub ::options-subscriptions-data
  (fn [[_ path] _]
    [(rf/subscribe [::component-type path])
     (rf/subscribe [:get (conj path :type)])])

  (fn [[component-type entity-type] [_ path]]
    (when (isa? component-type :heraldry.options/root)
      (let [context {:path path
                     :dispatch-value component-type
                     :entity-type entity-type}]
        (assoc context :required-subscriptions (options-subscriptions context))))))

(rf/reg-sub-raw ::options
  (fn [_app-db [_ path]]
    (reaction
     (when (seq path)
       (if-let [context @(rf/subscribe [::options-subscriptions-data path])]
         (-> context
             (assoc :subscriptions {:base-path path
                                    :data (into {}
                                                (map (fn [relative-path]
                                                       [relative-path
                                                        @(rf/subscribe [:get (vec (concat path relative-path))])]))
                                                (:required-subscriptions context))})
             options)
         (get @(rf/subscribe [::options (pop path)]) (last path)))))))

(defn render-option [key {:keys [render-options-path] :as context}]
  (get-sanitized-data (c/<< context :path (conj render-options-path key))))

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
  (cond
    (-> context :path last (= :field)) (c/-- context)
    (-> context :path drop-last last (= :elements)) (c/-- context 2)
    (-> context :path drop-last last (= :components)) (c/-- context 2)
    (-> context :path drop-last last (= :charges)) (c/-- context 4)
    (-> context :path last int?) (c/-- context 2)
    (cottise-context? context) (c/-- context 2)
    :else (do
            (log/warn :not-implemented "parent" context)
            nil)))

(defn- resolve-context [{:keys [path-map path]
                         :as context}]
  (if-let [real-path (get path-map path)]
    (-> context
        (c/<< :path real-path)
        (update :path-map dissoc path))
    context))

(defmulti properties effective-component-type)

(defmethod properties :default [context]
  (log/warn :not-implemented "interface.properties" context))

(rf/reg-sub-raw ::properties
  (fn [_app-db [_ context]]
    (reaction
     (properties context))))

(defn get-properties [context]
  @(rf/subscribe [::properties context]))

(defmulti environment (fn [_context properties]
                        (:type properties)))

(defmethod environment :default [_context _properties])

(defmulti subfield-environments (fn [_context properties]
                                  (:type properties)))

(defmethod subfield-environments nil [_context _properties])
(defmethod subfield-environments :default [context _properties]
  (log/warn :not-implemented 'subfield-environments context))

(defn- subfield-context [context]
  (let [subfield-index (-> context :path last)
        subfield? (and (isa? (effective-component-type context) :heraldry/field)
                       (int? subfield-index))]
    (when subfield?
      {:parent-context (c/-- context 2)
       :index subfield-index})))

(rf/reg-sub-raw ::environment
  (fn [_app-db [_ context]]
    (reaction
     (let [context (resolve-context context)]
       (if-let [subfield (subfield-context context)]
         (-> (subfield-environments (:parent-context subfield) (get-properties (:parent-context subfield)))
             :subfields
             (get (:index subfield)))
         (environment context (get-properties context)))))))

(defn get-parent-environment [{:keys [parent-environment
                                      path]
                               :as context}]
  (or (get parent-environment path)
      (if (cottise-context? context)
        @(rf/subscribe [::environment (parent (parent context))])
        @(rf/subscribe [::environment (parent context)]))))

(defn get-environment [context]
  @(rf/subscribe [::environment context]))

(defmulti render-shape (fn [_context properties]
                         (:type properties)))

(defmethod render-shape nil [_context _properties])

(defmulti subfield-render-shapes (fn [_context properties]
                                   (:type properties)))

(defmethod subfield-render-shapes nil [_context _properties])
(defmethod subfield-render-shapes :default [context _properties]
  (log/warn :not-implemented 'subfield-render-shapes context))

(rf/reg-sub-raw ::render-shape
  (fn [_app-db [_ context]]
    (reaction
     (let [context (resolve-context context)]
       (if-let [subfield (subfield-context context)]
         (-> (subfield-render-shapes (:parent-context subfield) (get-properties (:parent-context subfield)))
             :subfields
             (get (:index subfield)))
         (render-shape context (get-properties context)))))))

(defn get-render-shape [context]
  @(rf/subscribe [::render-shape context]))

(defmulti exact-shape (fn [_context properties]
                        (:type properties)))

(defn get-exact-shape [context]
  @(rf/subscribe [::exact-shape context]))

(defn get-exact-parent-shape [{:keys [parent-shape]
                               :as context}]
  (or parent-shape
      (if (cottise-context? context)
        @(rf/subscribe [::exact-shape (parent (parent context))])
        @(rf/subscribe [::exact-shape (parent context)]))))

(defn fallback-exact-shape [context]
  (let [shape-path (:shape (get-render-shape context))
        shape-path (if (vector? shape-path)
                     (first shape-path)
                     shape-path)]
    (field.environment/intersect-shapes
     shape-path
     (get-exact-shape (parent context)))))

(defn subfield-exact-shape [context {:keys [parent-context]}]
  (let [shape-path (:shape (get-render-shape context))
        shape-path (if (vector? shape-path)
                     (first shape-path)
                     shape-path)
        {:keys [reverse-transform-fn]} (get-properties parent-context)]
    (cond-> (field.environment/intersect-shapes
             shape-path
             (get-exact-shape (parent context)))
      reverse-transform-fn (->
                             path/parse-path
                             reverse-transform-fn
                             path/to-svg))))

(defmethod exact-shape nil [context _properties]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/ordinary [context _properties]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/charge [context _properties]
  ;; the charge dictates its own field, the parent field's shape does
  ;; not affect it like it does for ordinaries
  (:shape (get-render-shape context)))

(rf/reg-sub-raw ::exact-shape
  (fn [_app-db [_ context]]
    (reaction
     (let [context (resolve-context context)]
       (if-let [subfield (subfield-context context)]
         (subfield-exact-shape context subfield)
         (exact-shape context (get-properties context)))))))

(rf/reg-sub-raw ::field-edges
  (fn [_app-db [_ context]]
    (reaction
     (:edges (subfield-render-shapes context (get-properties context))))))

(defn get-field-edges [context]
  @(rf/subscribe [::field-edges context]))

(defmulti bounding-box (fn [_context properties]
                         (:type properties)))

(defmethod bounding-box :default [context _properties]
  (:bounding-box (get-environment context)))

(rf/reg-sub-raw ::bounding-box
  (fn [_app-db [_ context]]
    (reaction
     (let [context (resolve-context context)]
       (bounding-box context (get-properties context))))))

(defn get-bounding-box [context]
  @(rf/subscribe [::bounding-box context]))

(defn get-effective-parent-environment [context]
  (if (get-sanitized-data (c/++ context :inherit-environment?))
    (get-parent-environment (parent context))
    (get-parent-environment context)))

(defn get-effective-parent-shape [context]
  (if (get-sanitized-data (c/++ context :inherit-environment?))
    (get-exact-shape (parent (parent context)))
    (get-exact-shape (parent context))))
