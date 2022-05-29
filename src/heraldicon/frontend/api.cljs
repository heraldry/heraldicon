(ns heraldicon.frontend.api
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.http :as http]
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

(defn fetch-charge-for-rendering [charge-id version]
  (go
    (try
      (let [user-data (user/data)
            charge-data (<? (api.request/call :fetch-charge {:id charge-id
                                                             :version version} user-data))
            edn-data (<? (http/fetch (-> charge-data :data :edn-data-url)))]
        (update charge-data :data assoc :edn-data edn-data))
      (catch :default e
        (log/error "fetch charge for rendering error:" e)))))

(defn fetch-charge-for-editing [charge-id version]
  (go
    (try
      (let [user-data (user/data)
            charge-data (<? (api.request/call :fetch-charge {:id charge-id
                                                             :version version} user-data))
            edn-data (<? (http/fetch (-> charge-data :data :edn-data-url)))
            svg-data (<? (http/fetch (-> charge-data :data :svg-data-url)))]
        ;; currently need to fetch both, so saving a new version will send them again
        (update charge-data
                :data assoc
                :edn-data edn-data
                :svg-data svg-data))
      (catch :default e
        (log/error "fetch charge for editing error:" e)))))

(defn fetch-charges-list []
  (go
    (try
      (-> (api.request/call :fetch-charges-list {} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch charge list error:" e)))))

(defn fetch-charges-for-user [user-id]
  (go
    (try
      (-> (api.request/call :fetch-charges-for-user {:user-id user-id} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch charge list by user error:" e)))))

(defn fetch-arms [arms-id version target-path]
  (go
    (try
      (let [arms-data (<? (api.request/call :fetch-arms {:id arms-id
                                                         :version version} (user/data)))]
        (when target-path
          (rf/dispatch [:set target-path arms-data]))
        arms-data)
      (catch :default e
        (log/error "fetch arms error:" e)))))

(defn fetch-arms-list []
  (go
    (try
      (-> (api.request/call :fetch-arms-list {} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch arms list error:" e)))))

(defn fetch-arms-for-user [user-id]
  (go
    (try
      (-> (api.request/call :fetch-arms-for-user {:user-id user-id} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch arms list by user error:" e)))))
