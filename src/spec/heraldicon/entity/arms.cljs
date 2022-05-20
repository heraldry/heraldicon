(ns spec.heraldicon.entity.arms
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldicon.entity]
   [spec.heraldicon.entity.arms.data]))

(s/def :heraldicon.entity.arms/type #{:heraldicon.entity.type/arms})

(s/def :heraldicon.entity/arms (s/and :heraldicon/entity
                                      (s/keys :req-un [:heraldicon.entity.arms/type
                                                       :heraldicon.entity.arms/data])))
