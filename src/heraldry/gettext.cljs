(ns heraldry.gettext
  (:require [clojure.string :as s])
  (:require-macros [heraldry.gettext :refer [inline-dict]]))

(def DICT
  {:de (inline-dict "de_DE.po")})

(defn string [s]
  (-> DICT
      keys
      (->> (map (fn [k]
                  [k (get-in DICT [k s])]))
           (into {}))
      (assoc :en (s/replace s #" \[.*\]" ""))))
