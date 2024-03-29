(ns heraldicon.config
  (:refer-clojure :exclude [get])
  (:require
   [clojure.string :as str]
   [heraldicon.util.log-appender :as log-appender]
   [taoensso.timbre :as log]))

(defn- js->clj+
  "For cases when built-in js->clj doesn't work. Source: https://stackoverflow.com/a/32583549/4839573"
  [x]
  (into {}
        (map (fn [k]
               [(keyword k) (aget x k)]))
        (js-keys x)))

(def ^:private env
  "Returns current env vars as a Clojure map."
  (js->clj+ (.-env js/process)))

(goog-define stage "dev")
(goog-define commit "unknown")

(when (not= stage "dev")
  (log/swap-config! assoc :timestamp-opts {:pattern ""})
  (log/swap-config! assoc :appenders {:simple (log-appender/simple-appender {})}))

(def ^:private base-font-dir
  (if (= stage "dev")
    "/Library/Fonts"
    "/opt/fonts"))

(def ^:private config-data
  (case stage
    "dev" {:heraldicon-api-endpoint "http://localhost:4000/api"
           :heraldicon-url "http://localhost:8081"
           :heraldicon-site-url "http://localhost:4000/dev"
           :cognito-pool-config {:UserPoolId "eu-central-1_eHwF2byeJ"
                                 :ClientId "2v90eij0l4aluf2amqumqh9gko"
                                 :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_eHwF2byeJ/.well-known/jwks.json"}
           :static-files-url "http://localhost:8081"
           :fleur-de-lis-charge-id "charge:ZfqrIl"
           :torse-charge-id "charge:8vwlZ2"
           :helmet-charge-id "charge:hlsnvP"
           :compartment-charge-id "charge:SSLk9y"
           :supporter-charge-id "charge:fxOk19"
           :mantling-charge-id "charge:gTrIM7"}

    "staging" {:heraldicon-api-endpoint "https://lg0j98nuab.execute-api.eu-central-1.amazonaws.com/api"
               :heraldicon-discord-api-endpoint "https://lg0j98nuab.execute-api.eu-central-1.amazonaws.com/discord"
               :heraldicon-url "https://staging.heraldicon.org"
               :cognito-pool-config {:UserPoolId "eu-central-1_eHwF2byeJ"
                                     :ClientId "2v90eij0l4aluf2amqumqh9gko"
                                     :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_eHwF2byeJ/.well-known/jwks.json"}
               :static-files-url "https://cdn.staging.heraldicon.org"
               :fleur-de-lis-charge-id "charge:ZfqrIl"
               :torse-charge-id "charge:8vwlZ2"
               :helmet-charge-id "charge:hlsnvP"
               :compartment-charge-id "charge:SSLk9y"
               :supporter-charge-id "charge:fxOk19"
               :mantling-charge-id "charge:gTrIM7"}

    "prod" {:heraldicon-api-endpoint "https://2f1yb829vl.execute-api.eu-central-1.amazonaws.com/api"
            :heraldicon-discord-api-endpoint "https://2f1yb829vl.execute-api.eu-central-1.amazonaws.com/discord"
            :heraldicon-url "https://heraldicon.org"
            :cognito-pool-config {:UserPoolId "eu-central-1_WXqnJUEOT"
                                  :ClientId "21pvp6cc4l3gptoj4bl3jc9s7r"
                                  :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_WXqnJUEOT/.well-known/jwks.json"}
            :static-files-url "https://cdn.heraldicon.org"
            :fleur-de-lis-charge-id "charge:ZfqrIl"
            :torse-charge-id "charge:8vwlZ2"
            :helmet-charge-id "charge:hlsnvP"
            :compartment-charge-id "charge:SSLk9y"
            :supporter-charge-id "charge:fxOk19"
            :mantling-charge-id "charge:gTrIM7"}))

(defn get [setting]
  (case setting
    :stage stage
    :commit commit
    :region (or (:REGION env) "eu-central-1")
    :admins #{"or"}
    :maintenance-mode? false
    :base-font-dir base-font-dir
    :email-address "oliver@heraldicon.org"
    (or (some-> setting
                name
                str/upper-case
                (str/replace "-" "_")
                keyword
                env)
        (clojure.core/get config-data setting))))
