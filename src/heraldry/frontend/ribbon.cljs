(ns heraldry.frontend.ribbon
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [taoensso.timbre :as log]))

(defn fetch-ribbons-for-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-ribbons-for-user {:user-id user-id}
                              user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbons for user error:" e)))))

(defn fetch-ribbons []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-ribbons {}
                              user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbons error:" e)))))

(defn fetch-ribbon-for-rendering [ribbon-id version]
  (go
    (try
      (let [user-data (user/data)
            ribbon-data (<? (api-request/call :fetch-ribbon {:id ribbon-id
                                                             :version version} user-data))]
        ribbon-data)
      (catch :default e
        (log/error "fetch ribbon for rendering error:" e)))))

(defn fetch-ribbon-for-editing [ribbon-id version]
  (go
    (try
      (let [user-data (user/data)
            ribbon-data (<? (api-request/call :fetch-ribbon {:id ribbon-id
                                                             :version version} user-data))]
        ribbon-data)
      (catch :default e
        (log/error "fetch ribbon for editing error:" e)))))

(defn fetch-ribbon-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [db-path [:ribbon-data variant]
          [status ribbon-data] (state/async-fetch-data
                                db-path
                                variant
                                #(fetch-ribbon-for-rendering id version))]
      (when (= status :done)
        ribbon-data))
    (log/error "error fetching ribbon data, invalid variant:" variant)))

