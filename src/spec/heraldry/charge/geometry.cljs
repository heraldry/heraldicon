(ns spec.heraldry.charge.geometry
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.charge.geometry/size (s/nilable number?))
(s/def :heraldry.charge.geometry/stretch (s/nilable number?))
(s/def :heraldry.charge.geometry/mirrored? (s/nilable boolean?))
(s/def :heraldry.charge.geometry/reversed? (s/nilable boolean?))

(s/def :heraldry.charge/geometry (s/nilable (s/keys :opt-un [:heraldry.charge.geometry/size
                                                             :heraldry.charge.geometry/stretch
                                                             :heraldry.charge.geometry/mirrored?
                                                             :heraldry.charge.geometry/reversed?])))
