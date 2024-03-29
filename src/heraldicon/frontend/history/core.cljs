(ns heraldicon.frontend.history.core
  (:require
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.history.shared :as shared]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [re-frame.core :as rf]))

(defn register-undoable-path [path]
  (swap! shared/undoable-paths conj path))

(rf/reg-sub ::can-undo?
  (fn [[_ path] _]
    [(rf/subscribe [:get-list-size (shared/history-path path)])
     (rf/subscribe [:get (shared/index-path path)])])

  (fn [[history-length index] [_ _path]]
    (and (>= history-length index)
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

(defn- restore-state [db path index-fn]
  (let [index-path (shared/index-path path)
        history (get-in db (shared/history-path path))
        index (get-in db index-path)
        new-index (index-fn index)]
    (if (<= 0 new-index (-> history count dec))
      (-> db
          (update-in path (fn [previous-data]
                            (let [replacement (get history new-index)]
                              (assoc replacement
                                     :id (:id previous-data)
                                     :version (:version previous-data)))))
          (assoc-in index-path new-index)
          (tree/change-selected-component-if-removed path))
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
     [:i.fas.fa-undo.ui-icon {:title (tr :string.tooltip/undo)
                              :on-click (when can-undo?
                                          (js-event/handled
                                           #(rf/dispatch [::undo path])))
                              :class (when-not can-undo?
                                       "disabled")}]
     [:i.fas.fa-redo.ui-icon {:title (tr :string.tooltip/redo)
                              :on-click (when can-redo?
                                          (js-event/handled
                                           #(rf/dispatch [::redo path])))
                              :class (when-not can-redo?
                                       "disabled")}]]))
