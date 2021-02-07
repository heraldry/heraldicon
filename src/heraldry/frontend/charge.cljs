(ns heraldry.frontend.charge
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.state :as state]
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
      (let [user-data   (user/data)
            charge-data (<? (api-request/call :fetch-charge {:id      charge-id
                                                             :version version} user-data))
            edn-data    (<? (http/fetch (:edn-data-url charge-data)))]
        (-> charge-data
            (assoc-in [:data :edn-data] edn-data)
            (cond->
                (and (-> edn-data :colours)
                     (-> charge-data :colours not)) (->
                                                     (update-in [:data :edn-data] dissoc :colours)
                                                     (assoc :colours (-> edn-data :colours))))))
      (catch :default e
        (println "fetch-charge error:" e)))))

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
