(ns heraldicon.localization.locale
  (:require-macros [heraldicon.localization.locale :refer [load-locale]]))

(def ^:private JSON-DICT
  {:en (load-locale "en/strings.json")
   :de (load-locale "de/strings.json")
   :pt (load-locale "pt-PT/strings.json")
   :ru (load-locale "ru/strings.json")
   :uk (load-locale "uk/strings.json")})

(defn string [s]
  (into {}
        (map (fn [[k strings]]
               [k (get strings s)]))
        JSON-DICT))

(def all
  {:en :string.language/english
   :de :string.language/german
   :ru :string.language/russian
   :uk :string.language/ukrainian})
