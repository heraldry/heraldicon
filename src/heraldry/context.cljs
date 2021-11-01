(ns heraldry.context)

(defn ++ [context & args]
  (update context :path (comp vec concat) args))

(defn << [context key value]
  (assoc context key value))
