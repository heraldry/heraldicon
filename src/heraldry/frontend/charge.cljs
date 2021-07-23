(ns heraldry.frontend.charge
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [heraldry.interface :as interface]
            [taoensso.timbre :as log]))

(defn fetch-charges-for-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-charges-for-user {:user-id user-id}
                              user-data)
            <?
            :charges))
      (catch :default e
        (log/error "fetch charges for user error:" e)))))

(defn fetch-charges []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-charges {}
                              user-data)
            <?
            :charges))
      (catch :default e
        (log/error "fetch charges error:" e)))))

(defn fetch-charge-for-rendering [charge-id version]
  (go
    (try
      (let [user-data (user/data)
            charge-data (<? (api-request/call :fetch-charge {:id charge-id
                                                             :version version} user-data))
            edn-data (<? (http/fetch (:edn-data-url charge-data)))]
        (-> charge-data
            (assoc-in [:data] edn-data)))
      (catch :default e
        (log/error "fetch charge for rendering error:" e)))))

(defn fetch-charge-for-editing [charge-id version]
  (go
    (try
      (let [user-data (user/data)
            charge-data (<? (api-request/call :fetch-charge {:id charge-id
                                                             :version version} user-data))
            edn-data (<? (http/fetch (:edn-data-url charge-data)))
            svg-data (<? (http/fetch (:svg-data-url charge-data)))]
        (-> charge-data
            (assoc-in [:data :edn-data] edn-data)
            (assoc-in [:data :svg-data] svg-data)))
      (catch :default e
        (log/error "fetch charge for editing error:" e)))))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [db-path [:charge-data variant]
          [status charge-data] (state/async-fetch-data
                                db-path
                                variant
                                #(fetch-charge-for-rendering id version))]
      (when (= status :done)
        charge-data))
    (log/error "error fetching charge data, invalid variant:" variant)))

(defmethod interface/fetch-charge-data :frontend [_kind variant]
  (fetch-charge-data variant))
