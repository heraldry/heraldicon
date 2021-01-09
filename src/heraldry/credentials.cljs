(ns heraldry.credentials
  (:require ["@aws-sdk/credential-provider-ini" :refer [fromIni]]
            [heraldry.config :as config]))

(defn load []
  (when (= (config/get :stage)
           "local") (fromIni #js {:profile "heraldry-local-user"})))
