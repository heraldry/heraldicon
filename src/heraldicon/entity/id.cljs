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
    "arms" :heraldicon.entity/arms
    "charge" :heraldicon.entity/charge
    "ribbon" :heraldicon.entity/ribbon
    "collection" :heraldicon.entity/collection))
