(ns heraldicon.util.url
  (:require
   [clojure.string :as s]))

(defn domain [url]
  (-> url
      (s/split #"//" 2)
      last
      (s/split #"/" 2)
      first
      (s/split #":" 2)
      first))
