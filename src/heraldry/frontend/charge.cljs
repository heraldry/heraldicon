(ns heraldry.frontend.charge
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.user :as user]))

(defn fetch-charges-for-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-charges-for-user {:user-id user-id}
                              user-data)
            <?
            :charges))
      (catch :default e
        (println "fetch-charges-for-user error:" e)))))

(defn fetch-charges []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call :fetch-charges {}
                              user-data)
            <?
            :charges))
      (catch :default e
        (println "fetch-charges error:" e)))))

(defn fetch-charge [charge-id version]
  (go
    (try
      (let [user-data (user/data)
            response (<? (api-request/call :fetch-charge {:id charge-id
                                                          :version version} user-data))
            edn-data (<? (http/fetch (:edn-data-url response)))
            svg-data (<? (http/fetch (:svg-data-url response)))]
        (-> response
            (assoc-in [:data :edn-data] edn-data)
            (assoc-in [:data :svg-data] svg-data)))
      (catch :default e
        (println "fetch-charge error:" e)))))
