(ns heraldicon.util.username
  (:require
   [clojure.string :as str]))

;; Alphanumeric runs joined by single _ . - separators: a username must begin and end
;; with an alphanumeric character, and no two separators may be adjacent.
(def ^:private valid-re #"^[a-zA-Z0-9]+(?:[_.-][a-zA-Z0-9]+)*$")

(def min-length 3)
(def max-length 32)

(defn valid? [username]
  (and (not (str/blank? username))
       (<= min-length (count username) max-length)
       (some? (re-matches valid-re username))))
