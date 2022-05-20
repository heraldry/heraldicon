(ns spec.heraldicon.entity.collection.data
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.collection.data/type #{:heraldicon.entity.collection/data})

;; TODO: add data
(s/def :heraldicon.entity.collection/data (s/keys :req-un [:heraldicon.entity.collection.data/type]))
