(ns heraldicon.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.field.environment :as field.environment]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.options :as options]
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
  (component/effective-type (get-raw-data (c/++ context :type))))

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

(defn get-element-indices [{:keys [path] :as context}]
  (let [elements (if (-> path first (= :context))
                   (get-in context (drop 1 path))
                   @(rf/subscribe [:get path]))]
    (shield-separator/element-indices-with-position elements)))

;; TODO: this needs to be improved
(defn motto? [{:keys [path] :as context}]
  (-> (if (-> path first (= :context))
        (get-in context (drop 1 path))
        @(rf/subscribe [:get path]))
      :type
      (isa? :heraldry/motto)))

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

(defmulti environment effective-component-type)

(defmethod environment nil [context]
  (log/warn :not-implemented "interface.environment" context))

(rf/reg-sub-raw ::environment
  (fn [_app-db [_ context]]
    (reaction
     (environment context))))

(defn get-environment [context]
  @(rf/subscribe [::environment context]))

(defn parent [context]
  (cond
    (-> context :path last (= :field)) (c/-- context)
    (-> context :path last int?) (c/-- context 2)
    :else (do
            (log/warn :not-implemented "parent-environment" context)
            context)))

(defmulti properties effective-component-type)

(defmethod properties nil [context]
  (log/warn :not-implemented "interface.properties" context))

(rf/reg-sub-raw ::properties
  (fn [_app-db [_ context]]
    (reaction
     (properties context))))

(defn get-properties [context]
  @(rf/subscribe [::properties context]))

(defmulti render-shape effective-component-type)

(defmethod render-shape nil [context]
  (log/warn :not-implemented "interface.render-shape" context))

(rf/reg-sub-raw ::render-shape
  (fn [_app-db [_ context]]
    (reaction
     (render-shape context))))

(defn get-render-shape [context]
  @(rf/subscribe [::render-shape context]))

(defmulti exact-shape effective-component-type)

(defn get-exact-shape [context]
  @(rf/subscribe [::exact-shape context]))

(defn fallback-exact-shape [context]
  (field.environment/intersect-shapes
   (:shape (get-render-shape context))
   (get-exact-shape (parent context))))

(defmethod exact-shape nil [context]
  (fallback-exact-shape context))

(defmethod exact-shape :heraldry/ordinary [context]
  (fallback-exact-shape context))

(rf/reg-sub-raw ::exact-shape
  (fn [_app-db [_ context]]
    (reaction
     (exact-shape context))))
