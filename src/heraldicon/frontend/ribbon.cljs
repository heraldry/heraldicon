(ns heraldicon.frontend.ribbon
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]
   [taoensso.timbre :as log]))

(defn fetch-ribbons-for-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api.request/call :fetch-ribbons-for-user {:user-id user-id}
                              user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbons for user error:" e)))))

(defn fetch-ribbons []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api.request/call :fetch-ribbons-list {}
                              user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbons error:" e)))))

(defn fetch-ribbon [ribbon-id version]
  (go
    (try
      (let [user-data (user/data)
            ribbon-data (<? (api.request/call :fetch-ribbon {:id ribbon-id
                                                             :version version} user-data))]
        ribbon-data)
      (catch :default e
        (log/error "fetch ribbon for rendering error:" e)))))

(defn fetch-ribbon-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [db-path [:ribbon-data variant]
          [status ribbon-data] (state/async-fetch-data
                                db-path
                                variant
                                #(fetch-ribbon id version))]
      (when (= status :done)
        ribbon-data))
    (log/error "error fetching ribbon data, invalid variant:" variant)))
