(ns heraldry.util
  (:require [clojure.string :as s]
            [heraldry.config :as config]))

(def -current-id
  (atom 0))

(defn reset-id []
  (reset! -current-id 0))

(defn id [prefix]
  (str prefix "_" (swap! -current-id inc)))

(defn id-for-url [id]
  (-> id
      (s/split #":" 2)
      second))

(defn full-url-for-arms [arms-data]
  (let [version (:version arms-data)
        version (if (zero? version)
                  (:latest-version arms-data)
                  version)
        arms-id (-> arms-data
                    :id
                    id-for-url)]
    (str (config/get :armory-url) "/arms/" arms-id "/" version)))

(defn full-url-for-charge [charge-data]
  (let [version (:version charge-data)
        version (if (zero? version)
                  (:latest-version charge-data)
                  version)
        charge-id (-> charge-data
                      :id
                      id-for-url)]
    (str (config/get :armory-url) "/charges/" charge-id "/" version)))

(defn full-url-for-username [username]
  (str (config/get :armory-url) "/users/" username))
