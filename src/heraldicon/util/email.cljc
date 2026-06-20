(ns heraldicon.util.email
  (:require
   [clojure.string :as str]))

;; RFC 5321 caps the forward-path at 256 chars including the <>, so the address itself
;; maxes out at 254.
(def max-length 254)

(defn valid-email? [email]
  (and (not (str/blank? email))
       (<= (count email) max-length)
       (some? (re-matches #"^[^\s@]+@[^\s@]+\.[^\s@]+$" email))))
