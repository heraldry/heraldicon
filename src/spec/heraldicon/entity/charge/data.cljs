(ns spec.heraldicon.entity.charge.data
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.charge.data/type #{:heraldicon.entity.charge/data})

;; TODO: add data
(s/def :heraldicon.entity.charge/data (s/keys :req-un [:heraldicon.entity.charge.data/type]))
