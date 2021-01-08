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

(defn get [setting]
  (case setting
    :region                      (:REGION env)
    :stage                       (or (:STAGE env) (heraldry.config/get-static :stage))
    :table-charges               (:TABLE_CHARGES env)
    :table-users                 (:TABLE_USERS env)
    :table-sessions              (:TABLE_SESSIONS env)
    :charge-library-api-endpoint (heraldry.config/get-static :charge-library-api-endpoint)
    :dynamodb-endpoint           (:DYNAMODB_ENDPOINT env)
    nil))
