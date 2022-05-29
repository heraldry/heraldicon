(ns heraldicon.frontend.api
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn fetch-ribbon [ribbon-id version target-path]
  (go
    (try
      (let [ribbon-data (<? (api.request/call :fetch-ribbon {:id ribbon-id
                                                             :version version} (user/data)))]
        (when target-path
          (rf/dispatch [:set target-path ribbon-data]))
        ribbon-data)
      (catch :default e
        (log/error "fetch ribbon error:" e)))))

(defn fetch-ribbons-list []
  (go
    (try
      (-> (api.request/call :fetch-ribbons-list {} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch ribbon list error:" e)))))

(defn fetch-collection [collection-id version target-path]
  (go
    (try
      (let [collection-data (<? (api.request/call :fetch-collection {:id collection-id
                                                                     :version version} (user/data)))]
        (rf/dispatch [:set target-path collection-data])
        collection-data)
      (catch :default e
        (log/error "fetch collection error:" e)))))

(defn fetch-collections-list []
  (go
    (try
      (-> (api.request/call :fetch-collections-list {} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch collection list error:" e)))))

(defn fetch-collections-for-user [user-id]
  (go
    (try
      (-> (api.request/call :fetch-collections-for-user {:user-id user-id} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch collection list by user error:" e)))))
