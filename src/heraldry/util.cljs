(ns heraldry.util
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as s]
            [goog.crypt :as crypt]
            [goog.crypt.base64 :as b64]
            [heraldry.config :as config]
            [taoensso.timbre :as log]))

(def -current-id
  (atom 0))

(defn reset-id []
  (reset! -current-id 0))

(defn id [prefix]
  (str prefix "_" (swap! -current-id inc)))

(defn id-for-url [id]
  (when id
    (-> id
        (s/split #":" 2)
        second)))

(defn full-url-for-arms [arms-data]
  (when-let [arms-id (:id arms-data)]
    (let [version (:version arms-data)
          version (if (zero? version)
                    (:latest-version arms-data)
                    version)]
      (str (config/get :heraldry-url) "/arms/" (id-for-url arms-id) "/" version))))

(defn full-url-for-collection [collection-data]
  (when-let [collection-id (:id collection-data)]
    (let [version (:version collection-data)
          version (if (zero? version)
                    (:latest-version collection-data)
                    version)]
      (str (config/get :heraldry-url) "/collection/" (id-for-url collection-id) "/" version))))

(defn full-url-for-charge [charge-data]
  (when-let [charge-id (:id charge-data)]
    (let [version (:version charge-data)
          version (if (zero? version)
                    (:latest-version charge-data)
                    version)]
      (str (config/get :heraldry-url) "/charges/" (id-for-url charge-id) "/" version))))

(defn full-url-for-username [username]
  (str (config/get :heraldry-url) "/users/" username))

(defn choices->map [choices]
  (->> choices
       (map (fn [[group-name & items]]
              (if (and (-> items count (= 1))
                       (-> items first keyword?))
                ;; in this case there is no group, treat the first element of "items" as key
                ;; and "group-name" as display-name
                [[(first items) group-name]]
                (->> items
                     (map (comp vec reverse))))))
       (apply concat)
       (into {})))

(defn spy [value msg]
  (log/debug "spy:" msg)
  (pprint value)
  value)

(defn deep-merge-with [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

;; https://gist.github.com/jimweirich/1388782
(def roman-reductions
  '((1000 "M")
    (900 "CM") (500 "D") (400 "CD") (100 "C")
    (90 "XC") (50 "L") (40 "XL") (10 "X")
    (9 "IX") (5 "V") (4 "IV") (1 "I")))

(defn to-roman [number]
  (let [counts
        (map first
             (drop 1
                   (reductions (fn [[_ r] v]
                                 (list (int (/ r v))
                                       (- r (* v (int (/ r v))))))
                               (list 0 number)
                               (map first roman-reductions))))
        glyphs (map second roman-reductions)]
    (apply str
           (flatten
            (map (fn [[c g]] (take c (repeat g)))
                 (map vector counts glyphs))))))

(defn percent-of [base-value]
  (fn [v]
    (when v
      (-> v
          (* base-value)
          (/ 100)))))

(defn base64-decode-utf-8 [data]
  (-> data
      (b64/decodeStringToByteArray true)
      crypt/utf8ByteArrayToString))
