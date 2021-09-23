(ns heraldry.frontend.history.core
  (:require [heraldry.frontend.history.shared :as shared]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(defn can-undo? [db path]
  (let [history (get-in db (shared/history-path path))
        index (get-in db (shared/index-path path))]
    (and (-> history count (>= index))
         (pos? index))))

(defn can-redo? [db path]
  (let [history (get-in db (shared/history-path path))
        index (get-in db (shared/index-path path))]
    (-> history count dec (> index))))

(rf/reg-sub ::can-undo?
  (fn [db [_ path]]
    (can-undo? db path)))

(rf/reg-sub ::can-redo?
  (fn [db [_ path]]
    (can-redo? db path)))

(rf/reg-sub ::identifier-changed?
  (fn [db [_ path identifier]]
    (not= (get-in db (shared/identifier-path path)) identifier)))

(defn restore-state [db path index-fn]
  (let [index-path (shared/index-path path)
        history (get-in db (shared/history-path path))
        index (get-in db index-path)
        new-index (index-fn index)]
    (if (<= 0 new-index (-> history count dec))
      (-> db
          (assoc-in path (get history new-index))
          (assoc-in index-path new-index)
          (state/change-selected-component-if-removed path))
      db)))

(rf/reg-event-db ::clear
  (fn [db [_ path identifier]]
    (-> db
        (assoc-in (shared/history-path path) nil)
        (assoc-in (shared/index-path path) 0)
        (assoc-in (shared/identifier-path path) identifier))))

(rf/reg-event-db ::undo
  (fn [db [_ path]]
    (restore-state db path dec)))

(rf/reg-event-db ::redo
  (fn [db [_ path]]
    (restore-state db path inc)))

(defn buttons [path]
  (let [can-undo? @(rf/subscribe [::can-undo? path])
        can-redo? @(rf/subscribe [::can-redo? path])]
    [:div.history-buttons
     [:i.fas.fa-undo.ui-icon {:title "undo"
                              :on-click (when can-undo?
                                          #(state/dispatch-on-event % [::undo path]))
                              :class (when-not can-undo?
                                       "disabled")}]
     [:i.fas.fa-redo.ui-icon {:title "redo"
                              :on-click (when can-redo?
                                          #(state/dispatch-on-event % [::redo path]))
                              :class (when-not can-redo?
                                       "disabled")}]]))
