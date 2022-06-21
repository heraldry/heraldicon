(ns heraldicon.frontend.api
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.user :as user]
   [taoensso.timbre :as log]))

(defn fetch-collections-for-user [user-id]
  (go
    (try
      (-> (api.request/call :fetch-collections-for-user {:user-id user-id} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch collection list by user error:" e)))))

(defn fetch-charges-for-user [user-id]
  (go
    (try
      (-> (api.request/call :fetch-charges-for-user {:user-id user-id} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch charge list by user error:" e)))))

(defn fetch-arms-for-user [user-id]
  (go
    (try
      (-> (api.request/call :fetch-arms-for-user {:user-id user-id} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch arms list by user error:" e)))))
