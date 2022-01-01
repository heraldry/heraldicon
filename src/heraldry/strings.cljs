(ns heraldry.strings
  (:require
   [clojure.string :as s]
   [heraldry.static :as static])
  (:require-macros [heraldry.strings :refer [load-strings]]))

(def JSON-DICT
  {:en (load-strings "en-UK.json")
   :de (load-strings "de-DE.json")
   :pt (load-strings "pt-PT.json")
   :ru (load-strings "ru-RU.json")})

(defn string [s]
  (-> (->> JSON-DICT
           keys
           (map (fn [k]
                  [k (get-in JSON-DICT [k s])]))
           (into {}))
      (update :en #(s/replace % #" \[.*\]$" ""))))

(def known-languages
  {:en [:string.language/english (static/static-url "/img/flag-united-kingdom.svg")]
   :de [:string.language/german (static/static-url "/img/flag-germany.svg")]
   :ru [:string.language/russian (static/static-url "/img/flag-russia.svg")]})
