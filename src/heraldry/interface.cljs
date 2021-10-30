(ns heraldry.interface
  (:require
   [clojure.string :as s]
   [heraldry.coat-of-arms.counterchange :as counterchange]
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

(defn effective-component-type [path raw-type]
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

(defmulti component-options (fn [path data]
                              (effective-component-type path (:type data))))

(defmethod component-options nil [_path _data]
  nil)

(defn get-raw-data
  ([path context]
   (get-raw-data (assoc context :path path)))

  ([{:keys [path] :as context}]
   (if (-> path first (= :context))
     (get-in context (drop 1 path))
     @(rf/subscribe [:get-value path]))))

(defn get-options-by-context
  ([path context]
   (get-options-by-context (assoc context :path path)))

  ([{:keys [path] :as context}]
   (component-options path (get-raw-data path context))))

(defn get-relevant-options-by-context
  ([path context]
   (get-relevant-options-by-context (assoc context :path path)))

  ([{:keys [path] :as context}]
   (let [[options relative-path] (or (->> (range (count path) 0 -1)
                                          (keep (fn [idx]
                                                  (let [option-path (subvec path 0 idx)
                                                        relative-path (subvec path idx)
                                                        options (get-options-by-context option-path context)]
                                                    (when options
                                                      [options relative-path]))))
                                          first)
                                     [nil nil])]
     (get-in options relative-path))))

(defn get-element-indices
  ([path context]
   (get-element-indices (assoc context :path path)))

  ([{:keys [path] :as context}]
   (let [elements (if (-> path first (= :context))
                    (get-in context (drop 1 path))
                    @(rf/subscribe [:get-value path]))]
     (shield-separator/element-indices-with-position elements))))

;; TODO: this needs to be improved
(defn motto?
  ([path context]
   (motto? (assoc context :path path)))

  ([{:keys [path] :as context}]
   (-> (if (-> path first (= :context))
         (get-in context (drop 1 path))
         @(rf/subscribe [:get-value path]))
       :type
       #{:heraldry.motto.type/motto
         :heraldry.motto.type/slogan})))

(defn get-sanitized-data
  ([path context]
   (get-sanitized-data (assoc context :path path)))

  ([{:keys [path] :as context}]
   (if (-> path first (= :context))
     (let [data (get-raw-data path context)
           options (get-relevant-options-by-context path context)]
       (options/sanitize-value-or-data data options))
     @(rf/subscribe [:get-sanitized-data path]))))

(defn get-list-size
  ([path context]
   (get-list-size (assoc context :path path)))

  ([{:keys [path] :as context}]
   (if (-> path first (= :context))
     (count (get-in context (drop 1 path)))
     @(rf/subscribe [:get-list-size path]))))

(defn get-counterchange-tinctures
  ([path context]
   (get-counterchange-tinctures (assoc context :path path)))

  ([{:keys [path] :as context}]
   (if (-> path first (= :context))
     (-> (get-raw-data path context)
         (counterchange/get-counterchange-tinctures context))
     @(rf/subscribe [:get-counterchange-tinctures path context]))))

(defmulti fetch-charge-data (fn [kind _variant]
                              kind))

(defn render-option [key {:keys [render-options-path] :as context}]
  (get-sanitized-data (conj render-options-path key) context))

(defmulti render-component (fn [context]
                             (effective-component-type
                              (:path context)
                              ;; TODO: need the raw value here for type
                              (get-raw-data (update context :path conj :type)))))

(defmethod render-component nil [context]
  (log/warn :not-implemented "render-component" context)
  [:<>])

(defmulti blazon-component (fn [path context]
                             (effective-component-type
                              path
                              ;; TODO: need the raw value here for type
                              (get-raw-data (conj path :type) context))))

(defmethod blazon-component nil [path _context]
  (log/warn "blazon: unknown component" path)
  nil)

(defn blazon
  ([path context]
   (blazon (assoc context :path path)))

  ([{:keys [path] :as context}]
   (let [manual-blazon (get-sanitized-data (conj path :manual-blazon) context)]
     (if (-> manual-blazon count pos?)
       manual-blazon
       (blazon-component path context)))))
