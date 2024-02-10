(ns heraldicon.util.log-appender
  (:require
   [clojure.string :as str]))

(def ^:dynamic *request-id*
  nil)

(defn set-request-id
  [context]
  (set! *request-id* (:awsRequestId (js->clj context :keywordize-keys true))))

(defn simple-appender
  [{:as _appender-opts :keys []}]
  {:enabled? true
   :fn (fn [{:keys [output_]}]
         (let [message (str *request-id* " " (str/trim (force output_)))]
           (if js/process.stdout
             (js/process.stdout.write (str message "\n"))
             (js/console.log message))))})
