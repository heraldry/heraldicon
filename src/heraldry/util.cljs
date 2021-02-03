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
  (when id
    (-> id
        (s/split #":" 2)
        second)))

(defn full-url-for-arms [arms-data]
  (when-let [arms-id (:id arms-data)]
    (let [version (:version arms-data)
          version (if (zero? version)
                    (:latest-version arms-data)
                    version)]
      (str (config/get :heraldry-url) "/arms/" (id-for-url arms-id) "/" version))))

(defn full-url-for-charge [charge-data]
  (when-let [charge-id (:id charge-data)]
    (let [version (:version charge-data)
          version (if (zero? version)
                    (:latest-version charge-data)
                    version)]
      (str (config/get :heraldry-url) "/charges/" (id-for-url charge-id) "/" version))))

(defn full-url-for-username [username]
  (str (config/get :heraldry-url) "/users/" username))

(defn options->map [options]
  (->> options
       (map (fn [[group-name & items]]
              (if (and (-> items count (= 1))
                       (-> items first keyword?))
                ;; in this case there is no group, treat the first element of "items" as key
                ;; and "group-name" as display-name
                [[(first items) group-name]]
                (->> items
                     (map (comp vec reverse))))))
       (apply concat)
       (into {})))
