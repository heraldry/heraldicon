(ns heraldicon.frontend.entity.form
  (:require
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(def ^:private db-path-base
  [:forms])

(defn data-path [form-id]
  (conj db-path-base form-id :data))

(defn saved-data-path [form-id]
  (conj db-path-base form-id :saved-data))

(macros/reg-event-db ::clear
  (fn [db [_ form-id]]
    (update-in db db-path-base dissoc form-id)))

(rf/reg-sub ::unsaved-changes?
  (fn [[_ form-id] _]
    [(rf/subscribe [:get (data-path form-id)])
     (rf/subscribe [:get (saved-data-path form-id)])])

  (fn [[current saved] [_ form-id]]
    (case form-id
      :heraldicon.entity/arms (not= (assoc-in current [:data :achievement :render-options] nil)
                                    (assoc-in saved [:data :achievement :render-options] nil))
      (not= current saved))))
