(ns heraldry.gettext
  (:require [clojure.string :as s]
            [heraldry.static :as static])
  (:require-macros [heraldry.gettext :refer [inline-dict]]))

(def DICT
  {:de (inline-dict "de_DE.po")
   :ru (inline-dict "ru.po")})

(defn string [s]
  (-> DICT
      keys
      (->> (map (fn [k]
                  [k (get-in DICT [k s])]))
           (into {}))
      (assoc :en (s/replace s #" \[.*\]$" ""))))

(def known-languages
  {:en [(string "English") (static/static-url "/img/flag-united-kingdom.svg")]
   :de [(string "German") (static/static-url "/img/flag-germany.svg")]
   :ru [(string "Russian") (static/static-url "/img/flag-russia.svg")]})
