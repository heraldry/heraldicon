(ns spec.heraldry.charge.geometry
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.ordinary.geometry/size (s/nilable number?))
(s/def :heraldry.ordinary.geometry/stretch (s/nilable number?))
(s/def :heraldry.ordinary.geometry/mirrored? (s/nilable boolean?))
(s/def :heraldry.ordinary.geometry/reversed? (s/nilable boolean?))

(s/def :heraldry.ordinary/geometry (s/nilable (s/keys :opt-un [:heraldry.field.geometry/size
                                                               :heraldry.field.geometry/stretch
                                                               :heraldry.field.geometry/mirrored?
                                                               :heraldry.field.geometry/reversed?])))
