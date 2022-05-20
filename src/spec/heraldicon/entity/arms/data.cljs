(ns spec.heraldicon.entity.arms.data
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.arms.data/type #{:heraldicon.entity.arms/data})

(s/def :heraldicon.entity.arms/data (s/keys :req-un [:heraldicon.entity.arms.data/type
                                                     :heraldry/achievement]))
