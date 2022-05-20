(ns spec.heraldicon.entity.ribbon.data
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.ribbon.data/type #{:heraldicon.entity.ribbon/data})

;; TODO: add data
(s/def :heraldicon.entity.ribbon/data (s/keys :req-un [:heraldicon.entity.ribbon.data/type]))
