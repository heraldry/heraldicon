(ns heraldicon.util.walk)

(declare postwalk)

(defn- walk-map [f m]
  (let [changed (reduce (fn [changed entry]
                          (let [new-entry (postwalk f entry)]
                            (if (identical? entry new-entry)
                              changed
                              (assoc (or changed {}) (key entry) new-entry))))
                        nil
                        m)]
    (cond
      (nil? changed) m
      ;; nil drops an entry; a renamed key would move to the end with assoc,
      ;; so the map is rebuilt in its order then, as clojure.walk does
      (every? (fn [[k new-entry]]
                (or (nil? new-entry)
                    (= k (first new-entry))))
              changed) (reduce-kv (fn [m k new-entry]
                                    (if (nil? new-entry)
                                      (dissoc m k)
                                      (assoc m k (second new-entry))))
                                  m
                                  changed)
      :else (into (empty m)
                  (keep (fn [entry]
                          (get changed (key entry) entry)))
                  m))))

(defn- walk-items [f coll rebuild]
  (let [items (mapv #(postwalk f %) coll)]
    (if (every? true? (map identical? coll items))
      coll
      (rebuild items))))

(defn postwalk
  "Like clojure.walk/postwalk, but a node whose children come back unchanged
  is reused instead of rebuilt. A walk that changes only a few leaves then
  shares the rest of the tree with its input and allocates little."
  [f form]
  (f (cond
       (map-entry? form) (let [k (key form)
                               v (val form)
                               new-k (postwalk f k)
                               new-v (postwalk f v)]
                           (if (and (identical? k new-k)
                                    (identical? v new-v))
                             form
                             (MapEntry. new-k new-v nil)))
       (vector? form) (reduce-kv (fn [v i item]
                                   (let [new-item (postwalk f item)]
                                     (if (identical? item new-item)
                                       v
                                       (assoc v i new-item))))
                                 form
                                 form)
       (map? form) (walk-map f form)
       (list? form) (walk-items f form #(apply list %))
       (seq? form) (walk-items f form seq)
       (set? form) (walk-items f form #(into (empty form) %))
       :else form)))
