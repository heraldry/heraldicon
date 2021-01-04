(ns heraldry.charge-library.config
  (:refer-clojure :exclude [get])
  (:require-macros [heraldry.charge-library.config]))

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
    :region            (:REGION env)
    :stage             (or (:STAGE env) (heraldry.charge-library.config/get-static :stage))
    :table-charges     (:TABLE_CHARGES env)
    :table-users       (:TABLE_USERS env)
    :api-endpoint      (heraldry.charge-library.config/get-static :api-endpoint)
    :dynamodb-endpoint (:DYNAMODB_ENDPOINT env)
    nil))
