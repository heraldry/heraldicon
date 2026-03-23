(ns heraldicon.frontend.api
  (:require
   [cljs-http.client :as http]
   [clojure.walk :as walk]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]
   [heraldicon.config :as config]
   [heraldicon.frontend.user.session :as-alias session]
   [heraldicon.math.vector :as v]
   [heraldicon.util.encoding :as encoding]
   [re-frame.core :as rf]))

(defn call [name payload session]
  (go-catch
   (let [content-type "application/transit+json"
         accept "application/transit+json"
         {:keys [encode-fn]} (encoding/for-mimetype content-type)
         payload (walk/postwalk
                  (fn [v]
                    (if (instance? v/Vector v)
                      (into {} v)
                      v))
                  payload)
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
       (let [error (:error body)]
         (when (= (:type error) :client-not-logged-in)
           (rf/dispatch [::session/session-expired]))
         (if error
           (throw (ex-info "API error" error :api-error))
           (throw (ex-info (str "API error: " status) {:message body} :api-error))))))))
