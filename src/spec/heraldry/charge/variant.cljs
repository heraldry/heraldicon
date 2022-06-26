(ns spec.heraldry.charge.variant
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.charge.variant/id string?)
(s/def :heraldry.charge.variant/version (s/nilable number?))
(s/def :heraldry.charge/variant (s/keys :opt-un [:heraldry.charge.variant/version]
                                        :req-un [:heraldry.charge.variant/id]))
