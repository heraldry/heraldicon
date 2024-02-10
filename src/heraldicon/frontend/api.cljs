(ns heraldicon.frontend.api
  (:require
   [cljs-http.client :as http]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.config :as config]
   [heraldicon.util.encoding :as encoding]))

(defn call [name payload session]
  (go-catch
   (let [content-type "application/transit+json"
         accept "application/transit+json"
         {:keys [encode-fn]} (encoding/for-mimetype content-type)
         response (<? (http/post (config/get :heraldicon-api-endpoint)
                                 {:headers {"Session-Id" (:session-id session)
                                            "Content-Type" content-type
                                            "Accept" accept}
                                  :body (encode-fn {:call name
                                                    :data payload})}))
         status (:status response)
         body (:body response)]
     (if (= status 200)
       (:success body)
       (if-let [error (:error body)]
         (throw (ex-info "API error" error :api-error))
         (throw (ex-info (str "API error: " status) {:message body} :api-error)))))))
