(ns heraldry.config
  (:require [clojure.string :as s]))

(defn -js->clj+
  "For cases when built-in js->clj doesn't work. Source: https://stackoverflow.com/a/32583549/4839573"
  [x]
  (into {} (for [k (js-keys x)]
             [(keyword k) (aget x k)])))

(def env
  "Returns current env vars as a Clojure map."
  (-js->clj+ (.-env js/process)))

(goog-define stage "dev")
(goog-define commit "unknown")

(def config-data
  (case stage
    "dev" {:heraldry-api-endpoint "http://localhost:4000/api"
           :heraldry-url "http://localhost:8081"
           :cognito-pool-config {:UserPoolId "eu-central-1_eHwF2byeJ"
                                 :ClientId "2v90eij0l4aluf2amqumqh9gko"
                                 :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_eHwF2byeJ/.well-known/jwks.json"}
           :fleur-de-lis-charge-id "charge:RnHzw8"
           :torse-charge-id "charge:6A691Q"
           :helmet-charge-id "charge:Af7tBN"
           :static-files-url "http://localhost:8081"
           :base-font-dir "/Users/oliver/dev/armory/assets/font"}

    "staging" {:heraldry-api-endpoint "https://ru73nh6ozg.execute-api.eu-central-1.amazonaws.com/api"
               :heraldry-url "https://staging.heraldry.digital"
               :cognito-pool-config {:UserPoolId "eu-central-1_eHwF2byeJ"
                                     :ClientId "2v90eij0l4aluf2amqumqh9gko"
                                     :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_eHwF2byeJ/.well-known/jwks.json"}
               :fleur-de-lis-charge-id "charge:RnHzw8"
               :torse-charge-id "charge:6A691Q"
               :helmet-charge-id "charge:Af7tBN"
               :static-files-url "https://cdn.staging.heraldry.digital"
               :base-font-dir "./assets/font"}

    "prod" {:heraldry-api-endpoint "https://2f1yb829vl.execute-api.eu-central-1.amazonaws.com/api"
            :heraldry-url "https://heraldry.digital"
            :cognito-pool-config {:UserPoolId "eu-central-1_WXqnJUEOT"
                                  :ClientId "21pvp6cc4l3gptoj4bl3jc9s7r"
                                  :jwksUri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_WXqnJUEOT/.well-known/jwks.json"}
            :fleur-de-lis-charge-id "charge:ZfqrIl"
            :torse-charge-id "charge:8vwlZ2"
            :helmet-charge-id "charge:hlsnvP"
            :bucket-data "data.heraldry.digital"
            :static-files-url "https://cdn.heraldry.digital"
            :base-font-dir "./assets/font"}))

#_{:clj-kondo/ignore [:redefined-var]}
(defn get [setting]
  (case setting
    :stage stage
    :commit commit
    :region (or (:REGION env) "eu-central-1")
    :admins #{"or"}
    (or (some-> setting
                name
                s/upper-case
                (s/replace "-" "_")
                keyword
                env)
        (clojure.core/get config-data setting))))
