(ns heraldicon.localization.locale
  (:require-macros [heraldicon.localization.locale :refer [load-locale]]))

(def ^:private JSON-DICT
  {:en (load-locale "en-UK.json")
   :de (load-locale "de-DE.json")
   :pt (load-locale "pt-PT.json")
   :ru (load-locale "ru-RU.json")})

(defn string [s]
  (into {}
        (map (fn [[k strings]]
               [k (get strings s)]))
        JSON-DICT))

(def all
  {:en :string.language/english
   :de :string.language/german
   :ru :string.language/russian})
