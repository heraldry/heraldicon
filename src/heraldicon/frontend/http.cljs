(ns heraldicon.frontend.http
  (:require
   [cljs-http.client :as http]
   [cljs.reader :as reader]
   [clojure.string :as s]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]))

(defn fetch [url]
  (go-catch
   (-> (http/get url)
       <?
       (as-> response
             (let [status (:status response)
                   body (:body response)
                   data (if (and (s/ends-with? url ".edn")
                                 (string? body))
                          (reader/read-string body)
                          body)]
               (if (= status 200)
                 data
                 (throw (ex-info "HTTP request failed" {:error body} :fetch-url-failed))))))))
