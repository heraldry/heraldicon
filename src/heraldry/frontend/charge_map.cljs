(ns heraldry.frontend.charge-map
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]))

(defn fetch-charge-map []
  (go
    (try
      (let [user-data (user/data)]
        (<? (api-request/call :get-charge-map {} user-data)))
      (catch :default e
        (println "fetch-charge-map error:" e)))))

(defn fetch-charge [id version]
  (go
    (try
      (let [user-data (user/data)
            response  (<? (api-request/call :fetch-charge {:id      id
                                                           :version version} user-data))
            edn-data  (<? (http/fetch (:edn-data-url response)))]
        (-> response
            (assoc :data edn-data)))
      (catch :default e
        (println "fetch-charge error:" e)))))

(defn get-charge-map []
  (let [db-path             [:charge-map]
        [status charge-map] (state/async-fetch-data
                             db-path
                             :map
                             fetch-charge-map)]
    (when (= status :done)
      charge-map)))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [db-path              [:charge-data variant]
          [status charge-data] (state/async-fetch-data
                                db-path
                                variant
                                #(fetch-charge id version))]
      (when (= status :done)
        charge-data))
    (println "error fetching charge data, variant:" variant)))
