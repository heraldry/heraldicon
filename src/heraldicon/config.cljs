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

(def env
  "Returns current env vars as a Clojure map."
  (js->clj+ (.-env js/process)))

(goog-define stage "dev")
(goog-define commit "unknown")
(goog-define is_backend false)

(log/swap-config! assoc :appenders {:simple (log-appender/simple-appender {})})

(def ^:private config-data
  (case stage
    "dev" {:heraldicon-api-endpoint "http://localhost:8180/api"
           :heraldicon-url "http://localhost:8081"
           :heraldicon-site-url "http://localhost:8180"
           :cognito-pool-config {:UserPoolId "eu-central-1_eHwF2byeJ"
                                 :ClientId "7tcpem5hrjcu097d74h3nv6s92"
                                 :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_eHwF2byeJ/.well-known/jwks.json"}
           :static-files-url "http://localhost:8081"
           :fleur-de-lis-charge-id "charge:ZfqrIl"
           :torse-charge-id "charge:8vwlZ2"
           :helmet-charge-id "charge:hlsnvP"
           :compartment-charge-id "charge:SSLk9y"
           :supporter-charge-id "charge:fxOk19"
           :mantling-charge-id "charge:gTrIM7"
           :bucket-data "local-heraldry-data"}

    "staging" {:heraldicon-api-endpoint "/api"
               :heraldicon-url "https://staging.heraldicon.org"
               :cognito-pool-config {:UserPoolId "eu-central-1_eHwF2byeJ"
                                     :ClientId "7tcpem5hrjcu097d74h3nv6s92"
                                     :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_eHwF2byeJ/.well-known/jwks.json"}
               :static-files-url "https://cdn.staging.heraldicon.org"
               :fleur-de-lis-charge-id "charge:ZfqrIl"
               :torse-charge-id "charge:8vwlZ2"
               :helmet-charge-id "charge:hlsnvP"
               :compartment-charge-id "charge:SSLk9y"
               :supporter-charge-id "charge:fxOk19"
               :mantling-charge-id "charge:gTrIM7"
               :bucket-data "local-heraldry-data"}

    "prod" {:heraldicon-api-endpoint "/api"
            :heraldicon-discord-api-endpoint "https://2f1yb829vl.execute-api.eu-central-1.amazonaws.com/discord"
            :heraldicon-url "https://heraldicon.org"
            :cognito-pool-config {:UserPoolId "eu-central-1_WXqnJUEOT"
                                  :ClientId "2m6qhen3lsu9dpr7lb26697db8"
                                  :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_WXqnJUEOT/.well-known/jwks.json"}
            :static-files-url "https://cdn.heraldicon.org"
            :fleur-de-lis-charge-id "charge:ZfqrIl"
            :torse-charge-id "charge:8vwlZ2"
            :helmet-charge-id "charge:hlsnvP"
            :compartment-charge-id "charge:SSLk9y"
            :supporter-charge-id "charge:fxOk19"
            :mantling-charge-id "charge:gTrIM7"
            :bucket-data "prod-heraldry-data"}))

(defn get [setting]
  (case setting
    :stage stage
    :backend? is_backend
    :commit commit
    :region (or (:REGION env) "eu-central-1")
    :admins #{"or"}
    :maintenance-mode? false
    :email-address "oliver@heraldicon.org"
    (or (some-> setting
                name
                str/upper-case
                (str/replace "-" "_")
                keyword
                env)
        (clojure.core/get config-data setting))))
