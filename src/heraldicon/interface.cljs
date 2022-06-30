(ns heraldicon.interface
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.options :as options]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

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
    @(rf/subscribe [:heraldicon.state/options (:path context)])
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

(defn get-sanitized-data [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (let [data (get-raw-data context)
          options (get-relevant-options context)]
      (options/sanitize-value-or-data data options))
    @(rf/subscribe [:heraldicon.state/sanitized-data (:path context)])))

(defn get-list-size [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (count (get-in context (drop 1 path)))
    @(rf/subscribe [:get-list-size path])))

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
