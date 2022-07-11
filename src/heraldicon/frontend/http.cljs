(ns heraldicon.frontend.http
  (:require
   [cljs-http.client :as http]
   [cljs.reader :as reader]
   [clojure.string :as s]
   [com.wsscode.async.async-cljs :refer [<? go-catch]]))

(defn fetch [url]
  (go-catch
   (let [{:keys [status body]} (<? (http/get url))]
     (if (= status 200)
       (if (and (s/ends-with? url ".edn")
                (string? body))
         (reader/read-string body)
         body)
       (throw (ex-info "HTTP request failed" {:error body} :fetch-url-failed))))))
