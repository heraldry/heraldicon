(ns heraldicon.util.sanitize
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]))

(defn sanitize-string-or-nil [data]
  (some-> data
          (str/replace #"  *" " ")
          str/trim))

(defn sanitize-string [data]
  (or (sanitize-string-or-nil data)
      ""))

(defn sanitize-keyword [data]
  (-> (if (keyword? data)
        (name data)
        data)
      sanitize-string
      str/lower-case
      (str/replace #"[^a-z-]" "-")
      (str/replace #"^--*" "")
      (str/replace #"--*$" "")
      keyword))

(defn normalize-charge-type-name [s]
  (-> (or s "")
      str/trim
      (str/replace #"[^a-zA-Z0-9.\-' ]" "")
      (str/replace #"\s+" " ")
      str/trim))

(defn remove-nil-values-and-empty-maps [m]
  (walk/postwalk #(if (map? %)
                    (into {}
                          (remove (fn [[_ v]]
                                    (or (nil? v)
                                        (and (map? v)
                                             (empty? v)))))
                          %)
                    %)
                 m))
