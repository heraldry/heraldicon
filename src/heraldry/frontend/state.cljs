(ns heraldry.frontend.state
  (:require [clojure.string :as s]
            [re-frame.core :as rf]))

;; subs

(rf/reg-sub
 :get
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-sub
 :get-form-data
 (fn [db [_ form-id]]
   (get-in db [:form-data form-id])))

(rf/reg-sub
 :get-form-error-message
 (fn [db [_ form-id]]
   (get-in db [:form-error-message form-id])))

(rf/reg-sub
 :get-form-error
 (fn [db [_ form-id key]]
   (get-in db [:form-errors form-id key])))

;; events

(rf/reg-event-db
 :set
 (fn [db [_ path value]]
   (assoc-in db path value)))

(rf/reg-event-db
 :remove
 (fn [db [_ path]]
   (cond-> db
     (-> path count (= 1)) (dissoc (first path))
     (-> path count (> 1)) (update-in (drop-last path) dissoc (last path)))))

(rf/reg-event-db
 :set-form-data
 (fn [db [_ form-id data]]
   (assoc-in db [:form-data form-id] data)))

(rf/reg-event-db
 :set-form-data-key
 (fn [db [_ form-id key value]]
   (assoc-in db [:form-data form-id key] value)))

(rf/reg-event-db
 :set-form-error-message
 (fn [db [_ form-id message]]
   (assoc-in db [:form-error-message form-id] message)))

(rf/reg-event-db
 :set-form-error-key
 (fn [db [_ form-id key error]]
   (assoc-in db [:form-errors form-id key] error)))

(rf/reg-event-db
 :clear-form-errors
 (fn [db [_ form-id]]
   (update-in db [:form-errors] dissoc form-id)))

(rf/reg-event-db
 :clear-form
 (fn [db [_ form-id]]
   (-> db
       (update :form-data dissoc form-id)
       (update :form-error-message dissoc form-id)
       (update :form-errors dissoc form-id))))

;; other

(defn path []
  @(rf/subscribe [:get [:path]]))

(defn path-extra []
  (let [path-extra @(rf/subscribe [:get [:path-extra]])]
    (when (-> path-extra count (> 0))
      path-extra)))

(defn set-path [path & [hash]]
  (let [path       (if hash
                     (str path hash)
                     path)
        chunks     (s/split path #"#" 2)
        path       (first chunks)
        path-extra (second chunks)]
    (rf/dispatch-sync [:set [:path] path])
    (rf/dispatch-sync [:set [:path-extra] path-extra])))

(defn goto [path]
  (set-path path)
  (js/window.history.pushState {} nil path))
