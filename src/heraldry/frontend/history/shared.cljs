(ns heraldry.frontend.history.shared)

(def undo-path
  [:ui :undo])

(def undoable-paths
  [[:arms-form]
   [:charge-form]
   [:collection-form]
   [:ribbon-form]])

(def history-size
  200)

(defn history-path [path]
  (conj undo-path path :history))

(defn index-path [path]
  (conj undo-path path :index))

(defn identifier-path [path]
  (conj undo-path path :identifier))
