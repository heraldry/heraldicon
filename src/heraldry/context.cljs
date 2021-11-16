(ns heraldry.context)

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
