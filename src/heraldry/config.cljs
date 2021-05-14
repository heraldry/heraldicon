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

(goog-define region "eu-central-1")
(goog-define stage "local")

(goog-define heraldry-url "http://localhost:8081")
(goog-define heraldry-api-endpoint "http://localhost:4000/local/api")

(goog-define cognito-pool-id "eu-central-1_eHwF2byeJ")
(goog-define cognito-user-pool-id "2v90eij0l4aluf2amqumqh9gko")
(goog-define cognito-jwks-uri "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_eHwF2byeJ/.well-known/jwks.json")

(def cognito-pool-config
  {:UserPoolId cognito-pool-id
   :ClientId cognito-user-pool-id
   :jwksUri cognito-jwks-uri})

(goog-define fleur-de-lis-charge-id "charge:RnHzw8")

(goog-define bucket-data-override "")

(def bucket-data
  (if (-> bucket-data-override count pos?)
    bucket-data-override
    (:BUCKET_DATA env)))

(def admins
  #{"or"})

#_{:clj-kondo/ignore [:redefined-var]}
(defn get [setting]
  (some-> setting
          name
          s/upper-case
          (s/replace "-" "_")
          keyword
          env))
