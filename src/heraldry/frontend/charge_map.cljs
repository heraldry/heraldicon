(ns heraldry.frontend.charge-map
  (:require [cljs.core.async :refer [go <!]]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]))

(defn fetch-charge-map [db-path]
  (go
    (let [user-data (user/data)]
      (rf/dispatch-sync [:set db-path :loading])
      (-> (api-request/call :get-charge-map {} user-data)
          <!
          (as-> response
                (if-let [error (:error response)]
                  (println "fetch-charge-map error:" error)
                  (rf/dispatch [:set db-path response])))))))

(defn get-charge-map []
  (let [db-path [:charge-map]
        charge-map @(rf/subscribe [:get db-path])]
    (cond
      (nil? charge-map) (do
                          (fetch-charge-map db-path)
                          (rf/dispatch-sync [:set db-path :loading]))
      (= charge-map :loading) nil
      :else charge-map)))

(defn fetch-charge-data [charge]
  (go-catch
   (let [url (-> charge :edn-data-url)
         db-path [:charge-data url]
         charge-data @(rf/subscribe [:get db-path])]
     (cond
       (nil? charge-data) (do
                            (rf/dispatch-sync [:set db-path :loading])
                            (let [data (<? (http/fetch url))]
                              (rf/dispatch [:set db-path data])))
       (= charge-data :loading) nil
       :else charge-data))))
