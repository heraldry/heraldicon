(ns heraldicon.frontend.repository.user
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.request :as request]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-user
  (conj repository/db-path-base :user))

(defn- user-path [username]
  (conj db-path-user username))

(rf/reg-event-db ::store
  (fn [db [_ {:keys [username] :as user}]]
    (assoc-in db (user-path username) {:status :done
                                       :user user})))

(rf/reg-event-db ::store-error
  (fn [db [_ username error]]
    (assoc-in db (user-path username) {:status :error
                                       :error error})))

(defn- fetch [username session]
  (go
    (try
      (let [user (<? (request/call :fetch-user {:username username} session))]
        (when-not user
          (throw (ex-info "Not found" {} :user-not-found)))
        (rf/dispatch [::store user]))
      (catch :default e
        (log/error e "fetch user error")
        (rf/dispatch [::store-error username e])))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ username]]
    (reaction
     (let [session @(rf/subscribe [::session/data])]
       (repository/async-query-data (user-path username)
                                    (partial fetch username session))))))
