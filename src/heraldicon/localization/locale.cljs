(ns heraldicon.localization.locale
  (:require-macros [heraldicon.localization.locale :refer [load-locale]]))

(def JSON-DICT
  {:en (load-locale "en-UK.json")
   :de (load-locale "de-DE.json")
   :pt (load-locale "pt-PT.json")
   :ru (load-locale "ru-RU.json")})

(defn string [s]
  (->> JSON-DICT
       keys
       (map (fn [k]
              [k (get-in JSON-DICT [k s])]))
       (into {})))

(def all
  {:en :string.language/english
   :de :string.language/german
   :ru :string.language/russian})
