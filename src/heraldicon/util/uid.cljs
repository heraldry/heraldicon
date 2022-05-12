(ns heraldicon.util.uid)

(def -current-id
  (atom 0))

(defn reset []
  (reset! -current-id 0))

(defn generate [prefix]
  (str prefix "_" (swap! -current-id inc)))
