(ns spec.heraldicon.entity.ribbon
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldicon.entity]
   [spec.heraldicon.entity.ribbon.data]))

(s/def :heraldicon.entity.ribbon/type #{:heraldicon.entity.type/ribbon})

(s/def :heraldicon.entity/ribbon (s/and :heraldicon/entity
                                        (s/keys :req-un [:heraldicon.entity.ribbon/type
                                                         :heraldicon.entity.ribbon/data])))
