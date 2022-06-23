(ns heraldicon.frontend.message
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(def ^:private db-path-base
  [:messages])

(rf/reg-sub ::message
  (fn [db [_ message-id]]
    (get-in db (conj db-path-base message-id))))

(rf/reg-sub ::error?
  (fn [db [_ message-id]]
    (= (get-in db (conj db-path-base message-id :type))
       :error)))

(defn- set-message [db message-id type message]
  (assoc-in db (conj db-path-base message-id) {:type type
                                               :message message}))

(macros/reg-event-db ::set-success
  (fn [db [_ message-id message]]
    (set-message db message-id :success message)))

(macros/reg-event-db ::set-error
  (fn [db [_ message-id message]]
    (set-message db message-id :error message)))

(macros/reg-event-db ::clear
  (fn [db [_ message-id]]
    (update-in db db-path-base (fn [messages]
                                 (into {}
                                       (keep (fn [[k v]]
                                               (when-not (or (= k message-id)
                                                             (and (map? k)
                                                                  (-> k :parent (= message-id))))
                                                 [k v])))
                                       messages)))))

(defn display [message-id]
  (let [{:keys [type message]} @(rf/subscribe [::message message-id])]
    [:<>
     (when message
       [:div {:class (case type
                       :success "success-message"
                       :error "error-message")}
        [tr message]])]))
