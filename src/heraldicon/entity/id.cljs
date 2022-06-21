(ns heraldicon.entity.id
  (:require
   [clojure.string :as s]))

(defn for-url [id]
  (some-> id
          (s/split #":" 2)
          second))

(defn type-from-id [entity-id]
  (case (some-> entity-id
                (s/split #":")
                first)
    "arms" :heraldicon.entity.type/arms
    "charge" :heraldicon.entity.type/charge
    "ribbon" :heraldicon.entity.type/ribbon
    "collection" :heraldicon.entity.type/collection))
