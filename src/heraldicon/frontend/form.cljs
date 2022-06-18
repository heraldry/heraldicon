(ns heraldicon.frontend.form
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(def ^:private db-path-base
  [:forms])

(defn data-path [form-id]
  (conj db-path-base form-id :data))

(rf/reg-sub ::error
  (fn [db [_ form-id]]
    (get-in db (conj db-path-base form-id :error))))

(rf/reg-sub ::message
  (fn [db [_ form-id]]
    (get-in db (conj db-path-base form-id :message))))

(macros/reg-event-db ::set-error
  (fn [db [_ form-id message]]
    (assoc-in db (conj db-path-base form-id :error) message)))

(macros/reg-event-db ::set-message
  (fn [db [_ form-id message]]
    (assoc-in db (conj db-path-base form-id :message) message)))

(macros/reg-event-db ::clear-messages
  (fn [db [_ form-id]]
    (update-in db (conj db-path-base form-id) dissoc :error :message)))

(macros/reg-event-db ::clear
  (fn [db [_ form-id]]
    (update-in db db-path-base dissoc form-id)))

(defn messages [form-id]
  (let [error @(rf/subscribe [::error form-id])
        message @(rf/subscribe [::message form-id])]
    [:<>
     (when message
       [:div.success-message [tr message]])
     (when error
       [:div.error-message error])]))
