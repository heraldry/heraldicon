(ns heraldicon.frontend.entity.form
  (:require
   [heraldicon.frontend.macros :as macros]))

(def ^:private db-path-base
  [:forms])

(defn data-path [form-id]
  (conj db-path-base form-id :data))

(defn saved-data-path [form-id]
  (conj db-path-base form-id :saved-data))

(macros/reg-event-db ::clear
  (fn [db [_ form-id]]
    (update-in db db-path-base dissoc form-id)))
