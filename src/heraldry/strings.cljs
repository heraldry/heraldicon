(ns heraldry.strings
  (:require
   [heraldry.static :as static])
  (:require-macros [heraldry.strings :refer [load-strings]]))

(def JSON-DICT
  {:en (load-strings "en-UK.json")
   :de (load-strings "de-DE.json")
   :pt (load-strings "pt-PT.json")
   :ru (load-strings "ru-RU.json")})

(defn string [s]
  (->> JSON-DICT
       keys
       (map (fn [k]
              [k (get-in JSON-DICT [k s])]))
       (into {})))

(def known-languages
  {:en :string.language/english
   :de :string.language/german
   :ru :string.language/russian})
