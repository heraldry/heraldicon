(ns spec.heraldry.ornaments
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.ornament]))

(s/def :heraldry.ornaments/type #{:heraldry/ornaments})

(s/def :heraldry.ornaments/elements (s/coll-of :heraldry/ornament :into []))

(s/def :heraldry/ornaments (s/keys :req-un [:heraldry.ornaments/type]
                                   :opt-un [:heraldry.ornaments/elements]))
