(ns heraldicon.entity.id
  (:require [clojure.string :as s]))

(defn for-url [id]
  (some-> id
          (s/split #":" 2)
          second))
