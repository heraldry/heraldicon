(ns or.coad.util
  (:require [clojure.string :as s]))

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
