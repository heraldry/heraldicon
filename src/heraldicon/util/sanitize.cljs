(ns heraldicon.util.sanitize
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]))

(defn sanitize-string-or-nil [data]
  (some-> data
          (s/replace #"  *" " ")
          s/trim))

(defn sanitize-string [data]
  (or (sanitize-string-or-nil data)
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
