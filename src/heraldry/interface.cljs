(ns heraldry.interface
  (:require
   [heraldry.coat-of-arms.counterchange :as counterchange]
   [heraldry.component :as component]
   [heraldry.context :as c]
   [heraldry.options :as options]
   [heraldry.shield-separator :as shield-separator]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn get-raw-data [{:keys [path subscriptions] :as context}]
  (cond
    subscriptions (let [{:keys [base-path data]} subscriptions
                        relative-path (-> base-path count (drop path) vec)]
                    (if (contains? data relative-path)
                      (get data relative-path)
                      (log/error (str "Missing subscription: " path " context: " context))))
    (-> path first (= :context)) (get-in context (drop 1 path))
    :else @(rf/subscribe [:get path])))


(defn effective-component-type [context]
  (component/effective-type (:path context)
                            (get-raw-data (c/++ context :type))))

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

(defn new-options? [{:keys [path] :as _context}]
  (= (take 3 path) [:arms-form :coat-of-arms :field]))

(defn reduce-context [context]
  (-> context
      (dissoc :environment)
      (dissoc :blazonry)))

;; TODO: this is one of the biggest potential bottle necks
(defn get-relevant-options [{:keys [path] :as context}]
  (if (-> path first (not= :context))
    (if (new-options? context)
      @(rf/subscribe [:heraldry.state/options (reduce-context context)])
      @(rf/subscribe [:get-relevant-options path]))
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
      #{:heraldry.motto.type/motto
        :heraldry.motto.type/slogan}))

(defn get-sanitized-data [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (let [data (get-raw-data context)
          options (get-relevant-options context)]
      (options/sanitize-value-or-data data options))
    (if (new-options? context)
      @(rf/subscribe [:heraldry.state/sanitized-data (reduce-context context)])
      @(rf/subscribe [:get-sanitized-data path]))))

(defn get-list-size [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (count (get-in context (drop 1 path)))
    @(rf/subscribe [:get-list-size path])))

(defn get-counterchange-tinctures [{:keys [path] :as context}]
  (if (-> path first (= :context))
    (-> (get-raw-data context)
        (counterchange/get-counterchange-tinctures context))
    @(rf/subscribe [:get-counterchange-tinctures path context])))

(defmulti fetch-charge-data (fn [kind _variant]
                              kind))

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
