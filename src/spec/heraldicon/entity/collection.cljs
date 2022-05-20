(ns spec.heraldicon.entity.collection
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.collection/type #{:heraldicon.entity.type/collection})

(s/def :heraldicon.entity/collection (s/and :heraldicon/entity
                                            (s/keys :req-un [:heraldicon.entity.collection/type
                                                             :heraldicon.entity.collection/data])))
