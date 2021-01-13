(ns heraldry.util
  (:require ["crypto" :as crypto]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [clojure.pprint :refer [pprint]]
            [re-frame.core :as rf]))

(defn promise
  [resolver]
  (js/Promise. resolver))

(defn promise-from-callback
  [f]
  (promise (fn [resolve reject]
             (f (fn [error data]
                  (if (nil? error)
                    (resolve data)
                    (reject error)))))))

(defn spy [value msg]
  (println msg)
  (pprint value)
  value)

(defn map-keys
  "Applies f to each key of m. Also to keys of m's vals and so on."
  [f m]
  (zipmap
   (map (fn [k]
          (f k))
        (keys m))
   (map (fn [v]
          (if (map? v)
            (map-keys f v)
            v))
        (vals m))))

(defn iso-now []
  (->> (time/time-now)
       (format/unparse (:date-time format/formatters))))

(defn sha1 [data]
  (-> crypto
      (.createHash "sha1")
      (.update data)
      (.digest "hex")))

(defn dispatch [event effect]
  (rf/dispatch effect)
  (.stopPropagation event))

(defn dispatch-sync [event effect]
  (rf/dispatch-sync effect)
  (.stopPropagation event))

(def -current-id
  (atom 0))

(defn id [prefix]
  (str prefix "_" (swap! -current-id inc)))

(defn upper-case-first [s]
  (str (s/upper-case (or (first s) "")) (s/join (rest s))))

(defn translate [keyword]
  (when keyword
    (-> keyword
        name
        (s/replace "-" " "))))

(defn translate-tincture [keyword]
  (case keyword
    :none "[no tincture]"
    (translate keyword)))

(defn translate-line [{:keys [type]}]
  (when (not= type :straight)
    (translate type)))

(defn translate-cap-first [keyword]
  (-> keyword
      translate
      upper-case-first))

(defn combine [separator words]
  (s/join separator (filter #(> (count %) 0) words)))

(defn contains-in?
  [m ks]
  (not= ::absent (get-in m ks ::absent)))

(defn replace-recursively [data value replacement]
  (walk/postwalk #(if (= % value)
                    replacement
                    %)
                 data))
