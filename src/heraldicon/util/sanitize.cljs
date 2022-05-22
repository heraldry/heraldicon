(ns heraldicon.util.sanitize
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]))

(defn sanitize-string [data]
  (or (some-> data
              (s/replace #"  *" " ")
              s/trim)
      ""))

(defn sanitize-keyword [data]
  (-> (if (keyword? data)
        (name data)
        data)
      sanitize-string
      s/lower-case
      (s/replace #"[^a-z-]" "-")
      (s/replace #"^--*" "")
      (s/replace #"--*$" "")
      keyword))

(defn remove-nil-values [data]
  (walk/postwalk (fn [data]
                   (if (map? data)
                     (into {}
                           (keep (fn [[k v]]
                                   (when-not (nil? v)
                                     [k v])))
                           data)
                     data)) data))
