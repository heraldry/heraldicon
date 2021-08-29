(ns heraldry.frontend.history
  (:require [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

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

(defn add-new-state [path db new-db]
  (let [previous-state (get-in db path)
        new-state (get-in new-db path)]
    (if (or (= previous-state new-state)
            (not new-state))
      new-db
      (let [history-path (history-path path)
            index-path (index-path path)
            history (get-in new-db history-path [])
            index (get-in new-db index-path 0)
            new-history (-> history
                            (->> (take (inc index)))
                            vec
                            (conj new-state)
                            (->> (take-last history-size))
                            vec)
            new-index (-> new-history count dec)]
        (-> new-db
            (assoc-in history-path new-history)
            (assoc-in index-path new-index))))))

(defn add-new-states [db new-db]
  (loop [new-db new-db
         [path & rest] undoable-paths]
    (if path
      (recur (add-new-state path db new-db) rest)
      new-db)))

(defn can-undo? [db path]
  (let [history (get-in db (history-path path))
        index (get-in db (index-path path))]
    (and (-> history count (>= index))
         (pos? index))))

(defn can-redo? [db path]
  (let [history (get-in db (history-path path))
        index (get-in db (index-path path))]
    (-> history count dec (> index))))

(rf/reg-sub :can-undo?
  (fn [db [_ path]]
    (can-undo? db path)))

(rf/reg-sub :can-redo?
  (fn [db [_ path]]
    (can-redo? db path)))

(rf/reg-sub :identifier-changed?
  (fn [db [_ path identifier]]
    (not= (get-in db (identifier-path path)) identifier)))

(defn restore-state [db path index-fn]
  (let [index-path (index-path path)
        history (get-in db (history-path path))
        index (get-in db index-path)
        new-index (index-fn index)]
    (if (<= 0 new-index (-> history count dec))
      (-> db
          (assoc-in path (get history new-index))
          (assoc-in index-path new-index)
          (state/change-selected-component-if-removed path))
      db)))

(rf/reg-event-db :clear-history
  (fn [db [_ path identifier]]
    (-> db
        (assoc-in (history-path path) nil)
        (assoc-in (index-path path) 0)
        (assoc-in (identifier-path path) identifier))))

(rf/reg-event-db :undo
  (fn [db [_ path]]
    (restore-state db path dec)))

(rf/reg-event-db :redo
  (fn [db [_ path]]
    (restore-state db path inc)))

(defn buttons [path]
  (let [can-undo? @(rf/subscribe [:can-undo? path])
        can-redo? @(rf/subscribe [:can-redo? path])]
    [:div.history-buttons
     [:i.fas.fa-undo.ui-icon {:title "undo"
                              :on-click (when can-undo?
                                          #(state/dispatch-on-event % [:undo path]))
                              :class (when-not can-undo?
                                       "disabled")}]
     [:i.fas.fa-redo.ui-icon {:title "redo"
                              :on-click (when can-redo?
                                          #(state/dispatch-on-event % [:redo path]))
                              :class (when-not can-redo?
                                       "disabled")}]]))
