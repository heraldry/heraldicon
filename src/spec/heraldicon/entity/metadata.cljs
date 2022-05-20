(ns spec.heraldicon.entity.metadata
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldicon.entity.metadata/pair (s/tuple su/non-blank-string? su/non-blank-string?))

(s/def :heraldicon.entity/metadata (s/coll-of :heraldicon.entity.metadata/pair :into []))
