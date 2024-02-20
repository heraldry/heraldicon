(ns heraldicon.frontend.repository.blazonry-parser-data
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.frontend.repository.query :as query]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log])
  (:require-macros [reagent.ratom :refer [reaction]]))

(def ^:private db-path-blazonry-parser-data
  (conj repository/db-path-base :blazonry-parser-data))

(rf/reg-event-db ::store
  (fn [db [_ data]]
    (assoc-in db db-path-blazonry-parser-data {:status :done
                                               :data data
                                               :path (conj db-path-blazonry-parser-data :data)})))

(rf/reg-event-db ::store-error
  (fn [db [_ error]]
    (assoc-in db db-path-blazonry-parser-data {:status :error
                                               :error error})))

(rf/reg-event-db ::clear
  (fn [db [_]]
    (assoc-in db db-path-blazonry-parser-data nil)))

(defn- fetch [session on-fetch]
  (go
    (let [query-id [::fetch session]]
      (when-not (query/running? query-id)
        (query/register query-id)
        (try
          (let [data (<? (api/call :fetch-blazonry-parser-data {} session))]
            (when on-fetch
              (on-fetch data))
            (rf/dispatch [::store data]))
          (catch :default e
            (log/error e "fetch blazonry-parser-data error")
            (rf/dispatch [::store-error e]))
          (finally
            (query/unregister query-id)))))))

(rf/reg-sub-raw ::data
  (fn [_app-db [_ on-fetch]]
    (reaction
     (let [session @(rf/subscribe [::session/data])]
       (repository/async-query-data db-path-blazonry-parser-data (partial fetch session on-fetch))))))
