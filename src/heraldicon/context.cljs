(ns heraldicon.context)

;; Contexts are interned: stepping from the same context with the same key
;; returns the identical object. Contexts key the subscription caches, and
;; ClojureScript caches a map's hash in the map, so a reused context is hashed
;; once instead of on every subscription. The tables are WeakMaps keyed by the
;; parent, so they live only as long as the contexts they belong to.

;; context -> {key -> child}
(def ^:private child-contexts (js/WeakMap.))
;; child -> its parent, so -- can return the identical parent
(def ^:private parent-contexts (js/WeakMap.))

(defn- child [context k]
  (if (map? context)
    (let [by-key (.get child-contexts context)]
      (or (get by-key k)
          (let [result (update context :path #(conj (vec %) k))]
            (.set child-contexts context (assoc by-key k result))
            (.set parent-contexts result context)
            result)))
    (update context :path #(conj (vec %) k))))

(defn --
  ([context]
   (-- context 1))

  ([context number]
   (loop [context context
          number number]
     (if-let [parent (and (pos? number)
                          (map? context)
                          (.get parent-contexts context))]
       (recur parent (dec number))
       (cond-> context
         (pos? number) (update :path #(subvec % 0 (-> % count (- number) (max 0)))))))))

(defn ++ [context & args]
  (reduce child context args))

(defn << [context key value]
  (assoc context key value))

(defn add-counterchanged-path [context path]
  (update-in context [:render-hints :counterchanged-paths] conj path))

(defn counterchanged-paths [context]
  (-> context :render-hints :counterchanged-paths))

(defn add-counterchanged-tinctures [context [tincture-1 tincture-2]]
  (if (and tincture-1 tincture-2)
    (let [tincture-replacer {tincture-1 tincture-2
                             tincture-2 tincture-1}]
      (update-in context
                 [:render-hints :tincture-mapping]
                 (fn [tincture-mapping]
                   (let [new-mapping (into {}
                                           (map (fn [[k v]]
                                                  [k (get tincture-replacer v v)]))
                                           tincture-mapping)]
                     (cond-> new-mapping
                       (not (contains? new-mapping tincture-1)) (assoc tincture-1 tincture-2)
                       (not (contains? new-mapping tincture-2)) (assoc tincture-2 tincture-1))))))
    context))

(defn clear-tincture-mapping [context]
  (cond-> context
    (-> context :render-hints :tincture-mapping) (update :render-hints dissoc :tincture-mapping)))

(defn tincture-mapping [context]
  (-> context :render-hints :tincture-mapping))

;; context -> the same context without render hints
(def ^:private scrubbed (js/WeakMap.))

(defn scrub-render-hints [context]
  (if (and (map? context)
           (contains? context :render-hints))
    (or (.get scrubbed context)
        (let [result (dissoc context :render-hints)]
          (.set scrubbed context result)
          result))
    context))

(defn set-key [context key value]
  (assoc-in context [:component-data (:path context) key] value))

(defn get-key [context key]
  (get-in context [:component-data (:path context) key]))

(defn set-render-hint
  ([context key value]
   (assoc-in context [:render-hints key] value))
  ([context key value & kvs]
   (let [result (set-render-hint context key value)]
     (if kvs
       (if (next kvs)
         (recur result (first kvs) (second kvs) (nnext kvs))
         (throw (ex-info "Illegal argument for set-render-hint" {})))
       result))))

(defn clear-render-hint
  ([context key]
   (update context :render-hints dissoc key))
  ([context key & ks]
   (let [result (clear-render-hint context key)]
     (if ks
       (recur result (first ks) (next ks))
       result))))

(defn render-hints [context]
  (:render-hints context))

(defn remove-keys-for-children [{:keys [path]
                                 :as context}]
  (cond-> context
    (:component-data context) (update :component-data
                                      (fn [component-data]
                                        (into {}
                                              (remove (fn [[k _v]]
                                                        (and (not= path k)
                                                             (= (take (count path) k) path))))
                                              component-data)))))
