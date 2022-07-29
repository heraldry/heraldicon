(ns heraldicon.frontend.repository.query)

(def ^:private queries
  (atom #{}))

(defn register [id]
  (swap! queries conj id))

(defn running? [id]
  (contains? @queries id))

(defn unregister [id]
  (swap! queries disj id))
