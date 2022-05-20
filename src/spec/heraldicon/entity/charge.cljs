(ns spec.heraldicon.entity.charge
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.charge/type #{:heraldicon.entity.type/charge})

(s/def :heraldicon.entity/charge (s/and :heraldicon/entity
                                        (s/keys :req-un [:heraldicon.entity.charge/type
                                                         :heraldicon.entity.charge/data])))
