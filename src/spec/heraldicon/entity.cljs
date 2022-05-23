(ns spec.heraldicon.entity
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.entity :as entity]
   [spec.heraldicon.entity.attribution]
   [spec.heraldicon.entity.metadata]
   [spec.heraldicon.spec-util :as su]))

(try
  (derive :heraldicon.entity.type/arms :heraldicon/entity)
  (derive :heraldicon.entity.type/charge :heraldicon/entity)
  (derive :heraldicon.entity.type/collection :heraldicon/entity)
  (derive :heraldicon.entity.type/ribbon :heraldicon/entity)
  (catch :default _))

(s/def :heraldicon.entity/type #(isa? % :heraldicon/entity))
(s/def :heraldicon.entity/name su/non-blank-string?)
(s/def :heraldicon.entity/access (su/key-in? entity/access-map))
(s/def :heraldicon.entity/tags (s/map-of keyword? boolean?))

(s/def :heraldicon/entity (s/keys :req-un [:heraldicon.entity/type
                                           :heraldicon.entity/name]
                                  :opt-un [:heraldicon.entity/access
                                           :heraldicon.entity/attribution
                                           :heraldicon.entity/metadata
                                           :heraldicon.entity/tags]))
