(ns heraldicon.context)

(defn --
  ([context]
   (-- context 1))

  ([context number]
   (cond-> context
     (pos? number) (update :path #(subvec % 0 (-> % count (- number) (max 0)))))))

(defn ++ [context & args]
  (update context :path (comp vec concat) args))

(defn << [context key value]
  (assoc context key value))

(defn add-counterchanged-path [context path]
  (update-in context [:rendering :counterchanged-paths] conj path))

(defn counterchanged-paths [context]
  (-> context :rendering :counterchanged-paths))

(defn add-counterchanged-tinctures [context [tincture-1 tincture-2]]
  (if (and tincture-1 tincture-2)
    (let [tincture-replacer {tincture-1 tincture-2
                             tincture-2 tincture-1}]
      (update-in context
                 [:rendering :tincture-mapping]
                 (fn [tincture-mapping]
                   (let [new-mapping (into {}
                                           (map (fn [[k v]]
                                                  [k (get tincture-replacer v v)]))
                                           tincture-mapping)]
                     (cond-> new-mapping
                       (not (contains? new-mapping tincture-1)) (assoc tincture-1 tincture-2)
                       (not (contains? new-mapping tincture-2)) (assoc tincture-2 tincture-1))))))
    context))

(defn tincture-mapping [context]
  (-> context :rendering :tincture-mapping))

(defn scrub-rendering-context [context]
  (dissoc context :rendering))

(defn add-component-context [context component-context]
  (update-in context [:component-context (:path context)] merge component-context))

(defn component-context [context]
  (get-in context [:component-context (:path context)]))

(defn set-parent-environment-override [context environment]
  (add-component-context context {:parent-environment environment}))

(defn parent-environment-override [context]
  (get-in context [:component-context (:path context) :parent-environment]))
