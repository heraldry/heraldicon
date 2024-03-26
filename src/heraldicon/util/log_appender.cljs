(ns heraldicon.util.log-appender
  (:require
   [clojure.string :as str]))

(defn simple-appender
  [{:as _appender-opts :keys []}]
  {:enabled? true
   :fn (fn [{:keys [output_]}]
         (let [message (str/trim (force output_))]
           (if js/process.stdout
             (js/process.stdout.write (str message "\n"))
             (js/console.log message))))})
