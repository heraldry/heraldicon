(ns heraldry.frontend.history.core
  (:require
   [heraldry.frontend.history.shared :as shared]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [re-frame.core :as rf]))

(rf/reg-sub ::can-undo?
  (fn [[_ path] _]
    [(rf/subscribe [:get-list-size (shared/history-path path)])
     (rf/subscribe [:get (shared/index-path path)])])

  (fn [[history-length index] [_ _path]]
    (and (-> history-length (>= index))
         (pos? index))))

(rf/reg-sub ::can-redo?
  (fn [[_ path] _]
    [(rf/subscribe [:get-list-size (shared/history-path path)])
     (rf/subscribe [:get (shared/index-path path)])])

  (fn [[history-length index] [_ _path]]
    (-> history-length dec (> index))))

(rf/reg-sub ::identifier-changed?
  (fn [[_ path _] _]
    (rf/subscribe [:get (shared/identifier-path path)]))

  (fn [known-identifier [_ _ identifier]]
    (not= known-identifier identifier)))

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
     [:i.fas.fa-undo.ui-icon {:title (tr {:en "undo"
                                          :de "rückgängig"})
                              :on-click (when can-undo?
                                          #(state/dispatch-on-event % [::undo path]))
                              :class (when-not can-undo?
                                       "disabled")}]
     [:i.fas.fa-redo.ui-icon {:title (tr {:en "redo"
                                          :de "wiederholen"})
                              :on-click (when can-redo?
                                          #(state/dispatch-on-event % [::redo path]))
                              :class (when-not can-redo?
                                       "disabled")}]]))
