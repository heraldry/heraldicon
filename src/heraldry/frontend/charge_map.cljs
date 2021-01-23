(ns heraldry.frontend.charge-map
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]))

(defn fetch-charge-map [db-path]
  (go
    (try
      (let [user-data (user/data)]
        (rf/dispatch-sync [:set db-path :loading])
        (rf/dispatch [:set db-path (<? (api-request/call :get-charge-map {} user-data))]))
      (catch :default e
        (println "fetch-charge-map error:" e)))))

(defn get-charge-map []
  (let [db-path    [:charge-map]
        charge-map @(rf/subscribe [:get db-path])]
    (cond
      (nil? charge-map)       (do
                                (fetch-charge-map db-path)
                                (rf/dispatch-sync [:set db-path :loading]))
      (= charge-map :loading) nil
      :else                   charge-map)))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [db-path     [:charge-data variant]
          charge-data @(rf/subscribe [:get db-path])]
      (cond
        (nil? charge-data)       (do
                                   (rf/dispatch-sync [:set db-path :loading])
                                   (go
                                     (try
                                       (let [user-data (user/data)
                                             response  (<? (api-request/call :fetch-charge {:id      id
                                                                                            :version version} user-data))
                                             edn-data  (<? (http/fetch (:edn-data-url response)))]
                                         (rf/dispatch [:set db-path (-> response
                                                                        (assoc :data edn-data))]))
                                       (catch :default e
                                         (println "fetch-charge-data:" e))))
                                   nil)
        (= charge-data :loading) nil
        :else                    charge-data))
    (println "error fetching charge data, variant:" variant)))
