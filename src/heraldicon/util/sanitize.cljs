(ns heraldicon.util.sanitize
  (:require [clojure.string :as s]))

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
