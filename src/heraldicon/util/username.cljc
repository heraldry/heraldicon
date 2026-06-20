(ns heraldicon.util.username
  (:require
   [clojure.string :as str]))

;; Alphanumeric runs joined by single _ . - separators: a username must begin and end
;; with an alphanumeric character, and no two separators may be adjacent.
;; A length constraint will be added here once the legacy over/undersized names have
;; been migrated, so this stays the single source of truth for "is this valid?".
(def ^:private valid-re #"^[a-zA-Z0-9]+(?:[_.-][a-zA-Z0-9]+)*$")

(defn valid? [username]
  (and (not (str/blank? username))
       (some? (re-matches valid-re username))))
