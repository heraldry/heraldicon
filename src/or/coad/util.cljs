(ns or.coad.util
  (:require [clojure.string :as s]))

(defn upper-case-first [s]
  (str (s/upper-case (or (first s) "")) (s/join (rest s))))

(defn translate [keyword]
  (when keyword
    (-> keyword
        name
        (s/replace "-" " "))))
