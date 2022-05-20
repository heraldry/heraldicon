(ns spec.heraldry.charge.variant
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.charge.variant/id string?)
(s/def :heraldry.charge.variant/version number?)
(s/def :heraldry.charge/variant (s/nilable (s/keys :req-un [:heraldry.charge.variant/id
                                                            :heraldry.charge.variant/version])))
