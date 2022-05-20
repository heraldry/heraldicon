(ns spec.heraldicon.entity.arms
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.arms/type #{:heraldicon.entity.type/arms})

(s/def :heraldicon.entity/arms (s/and :heraldicon/entity
                                      (s/keys :req-un [:heraldicon.entity.arms/type
                                                       :heraldicon.entity.arms/data])))
