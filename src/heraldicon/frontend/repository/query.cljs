(ns heraldicon.frontend.repository.query)

(def ^:private queries
  (atom #{}))

(defn add [id]
  (swap! queries conj id))

(defn running? [id]
  (contains? @queries id))

(defn remove [id]
  (swap! queries disj id))
