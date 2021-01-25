(ns heraldry.api.request
  (:require [cljs-http.client :as http]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.frontend.config :as config]))

(defn call [name payload user-data]
  (println "calling API" name)
  (go-catch
   (let [response (<? (http/post (config/get :armory-api-endpoint)
                                 {:headers    {"Session-Id" (:session-id user-data)}
                                  :edn-params {:call name
                                               :data payload}}))
         status   (:status response)
         body     (:body response)]
     (if (= status 200)
       (:success body)
       (if (:error body)
         (throw (ex-info "API error" (:error body) :api-error))
         (throw (ex-info (str "API error: " status) {:message body} :api-error)))))))
