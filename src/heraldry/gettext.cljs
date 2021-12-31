(ns heraldry.gettext
  (:require
   [clojure.string :as s]
   [heraldry.static :as static])
  (:require-macros [heraldry.gettext :refer [inline-dict inline-dict-json]]))

(def DICT
  {:de (inline-dict "de-DE.po")
   :ru (inline-dict "ru-RU.po")})

(def JSON-DICT
  {:en (inline-dict-json "en-UK.json")})

(defn string [s]
  (if (keyword? s)
    (-> (->> JSON-DICT
             keys
             (map (fn [k]
                    [k (get-in JSON-DICT [k s])]))
             (into {}))
        (update :en #(s/replace % #" \[.*\]$" "")))
    (-> DICT
        keys
        (->> (map (fn [k]
                    [k (get-in DICT [k s])]))
             (into {}))
        (assoc :en (s/replace s #" \[.*\]$" "")))))

(def known-languages
  {:en [:string.language/english (static/static-url "/img/flag-united-kingdom.svg")]
   :de [:string.language/german (static/static-url "/img/flag-germany.svg")]
   :ru [:string.language/russian (static/static-url "/img/flag-russia.svg")]})
