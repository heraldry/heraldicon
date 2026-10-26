(ns heraldicon.localization.locale
  (:require-macros [heraldicon.localization.locale :refer [load-locale]]))

;; English is bundled as the fallback, the frontend fetches other languages when they are selected
(def ^:private english
  (load-locale "en/strings.json"))

(defonce ^:private dictionaries
  (atom {}))

(defn- flatten-keys [data prefix]
  (into {}
        (mapcat (fn [[k v]]
                  (if (map? v)
                    (flatten-keys v (str prefix "." k))
                    [[(keyword prefix k) v]])))
        data))

(defn loaded? [language]
  (or (= language :en)
      (contains? @dictionaries language)))

(defn register! [language ^js data]
  (swap! dictionaries assoc language (flatten-keys (js->clj data) "string")))

(defn string [language key]
  (if (= language :en)
    (get english key)
    (get-in @dictionaries [language key])))

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
