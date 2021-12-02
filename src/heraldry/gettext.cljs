(ns heraldry.gettext
  (:require-macros [heraldry.gettext :refer [inline-dict]]))

(def DICT
  {:de (inline-dict "de_DE.po")
   :en (inline-dict "en_GB.po")})

(defn string [s]
  (-> DICT
      keys
      (->> (map (fn [k]
                  [k (get-in DICT [k s])]))
           (into {}))
      (update :en (fn [en-s]
                    (if (-> en-s count pos?)
                      en-s
                      s)))))
