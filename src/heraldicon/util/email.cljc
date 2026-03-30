(ns heraldicon.util.email
  (:require
   [clojure.string :as str]))

(defn valid-email? [email]
  (and (not (str/blank? email))
       (some? (re-matches #"^[^\s@]+@[^\s@]+\.[^\s@]+$" email))))
