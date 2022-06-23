(ns heraldicon.frontend.repository.user-list
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-user-list
  (conj repository/db-path-base :user-list))

(rf/reg-event-db ::store
  (fn [db [_ users]]
    (assoc-in db db-path-user-list {:status :done
                                    :users users
                                    :path (conj db-path-user-list :users)})))

(rf/reg-event-db ::store-error
  (fn [db [_ error]]
    (assoc-in db db-path-user-list {:status :error
                                    :error error})))

(rf/reg-event-db ::clear
  (fn [db [_]]
    (assoc-in db db-path-user-list nil)))

(defn- fetch [session]
  (go
    (try
      (let [users (:items (<? (request/call :fetch-users-all {} session)))]
        (rf/dispatch [::store users]))
      (catch :default e
        (log/error "fetch user list error:" e)
        (rf/dispatch [::store-error e])))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_]]
    (reaction
     (let [session @(rf/subscribe [::session/data])]
       (repository/async-query-data db-path-user-list (partial fetch session))))))
