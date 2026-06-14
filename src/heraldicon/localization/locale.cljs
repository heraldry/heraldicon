(ns heraldicon.localization.locale
  (:require-macros [heraldicon.localization.locale :refer [load-locale]]))

(def ^:private JSON-DICT
  {:en (load-locale "en/strings.json")
   :de (load-locale "de/strings.json")
   :es (load-locale "es/strings.json")
   :fr (load-locale "fr/strings.json")
   :it (load-locale "it/strings.json")
   :pt (load-locale "pt/strings.json")
   :ru (load-locale "ru/strings.json")
   :uk (load-locale "uk/strings.json")})

(defn string [s]
  (into {}
        (map (fn [[k strings]]
               [k (get strings s)]))
        JSON-DICT))

(def all
  {:en "English"
   :fr "Français"
   :de "Deutsch"
   :es "Español"
   :it "Italiano"
   ;; not yet enough strings translated, so disabled for now
   #_#_:pt "Português"
   :ru "Русский"
   :uk "Українська"})
