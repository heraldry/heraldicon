(ns heraldicon.frontend.repository.api
  (:require
   [cljs-http.client :as http]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.config :as config]))

(defn call [name payload session]
  (go-catch
   (let [response (<? (http/post (config/get :heraldicon-api-endpoint)
                                 {:headers {"Session-Id" (:session-id session)}
                                  :edn-params {:call name
                                               :data payload}}))
         status (:status response)
         body (:body response)]
     (if (= status 200)
       (:success body)
       (if-let [error (:error body)]
         (throw (ex-info "API error" error :api-error))
         (throw (ex-info (str "API error: " status) {:message body} :api-error)))))))
