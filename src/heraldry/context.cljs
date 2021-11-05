(ns heraldry.context)

(defn --
  ([context]
   (-- context 1))

  ([context number]
   (assert (pos? number))
   (update context :path #(subvec % 0 (max 0 (- (count %) number))))))

(defn ++ [context & args]
  (update context :path (comp vec concat) args))

(defn << [context key value]
  (assoc context key value))
