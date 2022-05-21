(ns spec.heraldicon.entity.ribbon.data
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.ribbon]))

(s/def :heraldicon.entity.ribbon.data/type #{:heraldicon.entity.ribbon/data})

(s/def :heraldicon.entity.ribbon.data/ribbon :heraldry/ribbon)

(s/def :heraldicon.entity.ribbon/data (s/keys :req-un [:heraldicon.entity.ribbon.data/type
                                                       :heraldicon.entity.ribbon.data/ribbon]))
