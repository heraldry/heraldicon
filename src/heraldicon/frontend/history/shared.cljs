(ns heraldicon.frontend.history.shared)

(def ^:private undo-path
  [:ui :undo])

(def undoable-paths
  (atom #{}))

(def history-size
  200)

(defn history-path [path]
  (conj undo-path path :history))

(defn index-path [path]
  (conj undo-path path :index))

(defn identifier-path [path]
  (conj undo-path path :identifier))
