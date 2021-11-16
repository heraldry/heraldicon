(ns heraldry.interface
  (:require
   [clojure.string :as s]
   [heraldry.coat-of-arms.counterchange :as counterchange]
   [heraldry.context :as c]
   [heraldry.options :as options]
   [heraldry.shield-separator :as shield-separator]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.component") t
      (s/starts-with? ts ":heraldry.field") :heraldry.component/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.component/ordinary
      (s/starts-with? ts ":heraldry.charge-group") :heraldry.component/charge-group
      (s/starts-with? ts ":heraldry.charge") :heraldry.component/charge
      (s/starts-with? ts ":heraldry.ribbon.segment") :heraldry.component/ribbon-segment
      (s/starts-with? ts ":heraldry.motto") :heraldry.component/motto
      :else nil)))

(defn get-raw-data [{:keys [path subscriptions] :as context}]
  (cond
    (-> path first (= :context)) (get-in context (drop 1 path))
    (-> path first (= :subscriptions)) (let [relative-path (drop 1 path)]
                                         (if (contains? subscriptions relative-path)
                                           (get subscriptions relative-path)
                                           (log/error (str "Missing subscription: " path " context: " context))))
    :else @(rf/subscribe [:get path])))

(defn raw-effective-component-type [path raw-type]
  (cond
    (-> path last (= :arms-form)) :heraldry.component/arms-general
    (-> path last #{:charge-form
                    :charge-data}) :heraldry.component/charge-general
    (-> path last (= :collection-form)) :heraldry.component/collection-general
    (-> path last (= :collection)) :heraldry.component/collection
    (->> path drop-last (take-last 2) (= [:collection :elements])) :heraldry.component/collection-element
    (-> path last (= :render-options)) :heraldry.component/render-options
    (-> path last (= :helms)) :heraldry.component/helms
    (-> path last (= :ribbon-form)) :heraldry.component/ribbon-general
    (-> path last (= :coat-of-arms)) :heraldry.component/coat-of-arms
    (-> path last (= :ornaments)) :heraldry.component/ornaments
    (keyword? raw-type) (type->component-type raw-type)
    (and (-> path last keyword?)
         (-> path last name (s/starts-with? "cottise"))) :heraldry.component/cottise
    :else nil))

(defn effective-component-type [context]
  (raw-effective-component-type (:path context)
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

;; TODO: this is one of the biggest potential bottle necks
(defn get-relevant-options [{:keys [path] :as context}]
  (if (-> path first (not= :context))
    @(rf/subscribe [:get-relevant-options path])
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
    @(rf/subscribe [:get-sanitized-data path])))

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
