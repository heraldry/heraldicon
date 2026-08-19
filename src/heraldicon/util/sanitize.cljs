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

(defn filename
  "Turn an entity name into a safe download filename base, falling back to
   `fallback` when the name is blank. Strips control characters and characters
   that are invalid in filenames or would break a Content-Disposition header."
  [data fallback]
  (let [base (-> (or data "")
                 (str/replace #"[\x00-\x1f\x7f]" "")
                 (str/replace #"[\\/:*?\"<>|]" "")
                 (str/replace #"\s+" " ")
                 str/trim)]
    (if (str/blank? base)
      fallback
      base)))

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
