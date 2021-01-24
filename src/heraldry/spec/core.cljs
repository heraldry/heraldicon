(ns heraldry.spec.core)

(defn get-key [coll key]
  (or (get coll key)
      (get coll (-> key name keyword))))
