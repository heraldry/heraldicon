(ns heraldry.frontend.http
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [go <!]]
            [cljs.reader :as reader]
            [clojure.string :as s]))

(defn fetch [url]
  (go
    (-> (http/get url)
        <!
        (as-> response
            (let [status (:status response)
                  body   (:body response)
                  data   (if (and (s/ends-with? url ".edn")
                                  (string? body))
                           (reader/read-string body)
                           body)]
              (if (= status 200)
                (do
                  (println "retrieved" url)
                  data)
                (do
                  (println "error fetching" url)
                  (throw (ex-info "HTTP request failed" {:body body} :fetch-url-failed)))))))))
