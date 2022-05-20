(ns spec.heraldicon.entity
  (:require
   [cljs.spec.alpha :as s]
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
(s/def :heraldicon.entity/is-public (s/nilable boolean?))
(s/def :heraldicon.entity/tags (s/nilable (s/map-of keyword? boolean?)))

(s/def :heraldicon/entity (s/keys :req-un [:heraldicon.entity/type
                                           :heraldicon.entity/name]
                                  :opt-un [:heraldicon.entity/is-public
                                           :heraldicon.entity/attribution
                                           :heraldicon.entity/metadata
                                           :heraldicon.entity/tags]))
