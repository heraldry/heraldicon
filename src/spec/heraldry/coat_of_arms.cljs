(ns spec.heraldry.coat-of-arms
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.coat-of-arms/type #{:heraldry/coat-of-arms})
(s/def :heraldry.coat-of-arms/field :heraldry/field)

(s/def :heraldry.coat-of-arms/manual-blazon (s/nilable string?))

(s/def :heraldry/coat-of-arms (s/keys :req-un [:heraldry.coat-of-arms/type
                                               :heraldry.coat-of-arms/field]
                                      :opt-un [:heraldry.coat-of-arms/manual-blazon]))
