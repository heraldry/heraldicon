(ns heraldicon.frontend.history.state
  (:require
   [heraldicon.frontend.history.shared :as shared]))

(defn- remove-ignored [data]
  (dissoc data :id :version :created-at))

(defn- add-new-state [path db new-db]
  (let [previous-state (remove-ignored (get-in db path))
        new-state (remove-ignored (get-in new-db path))]
    (if (or (= previous-state new-state)
            (not new-state))
      new-db
      (let [history-path (shared/history-path path)
            index-path (shared/index-path path)
            history (get-in new-db history-path [])
            index (get-in new-db index-path 0)
            new-history (-> history
                            (->> (take (inc index)))
                            vec
                            (conj new-state)
                            (->> (take-last shared/history-size))
                            vec)
            new-index (-> new-history count dec)]
        (-> new-db
            (assoc-in history-path new-history)
            (assoc-in index-path new-index))))))

(defn ^:export add-new-states [db new-db]
  (loop [new-db new-db
         [path & rest] @shared/undoable-paths]
    (if path
      (recur (add-new-state path db new-db) rest)
      new-db)))
