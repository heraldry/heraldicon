(ns spec.heraldicon.spec-util
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as str]))

(defn key-in? [m]
  (-> m keys set))

(def non-blank-string?
  (s/and string? (complement str/blank?)))

(def pos-number?
  (s/and number? (complement neg?)))
