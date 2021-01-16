(ns heraldry.config
  (:refer-clojure :exclude [get])
  (:require-macros [heraldry.config]))

(defn -js->clj+
  "For cases when built-in js->clj doesn't work. Source: https://stackoverflow.com/a/32583549/4839573"
  [x]
  (into {} (for [k (js-keys x)]
             [(keyword k) (aget x k)])))

(def env
  "Returns current env vars as a Clojure map."
  (-js->clj+ (.-env js/process)))

(def stage
  (or (:STAGE env) (heraldry.config/get-static :stage)))

(defn get [setting]
  (case setting
    :region              (:REGION env)
    :stage               stage
    :table-charges       (:TABLE_CHARGES env)
    :table-arms          (:TABLE_ARMS env)
    :table-users         (:TABLE_USERS env)
    :table-sessions      (:TABLE_SESSIONS env)
    :bucket-charges      (:BUCKET_CHARGES env)
    :bucket-arms         (:BUCKET_ARMS env)
    :armory-api-endpoint (heraldry.config/get-static :armory-api-endpoint)
    nil))
