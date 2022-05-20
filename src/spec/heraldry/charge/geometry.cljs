(ns spec.heraldry.charge.geometry
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.charge.geometry/size number?)
(s/def :heraldry.charge.geometry/stretch number?)
(s/def :heraldry.charge.geometry/mirrored? boolean?)
(s/def :heraldry.charge.geometry/reversed? boolean?)

(s/def :heraldry.charge/geometry (s/keys :opt-un [:heraldry.charge.geometry/size
                                                  :heraldry.charge.geometry/stretch
                                                  :heraldry.charge.geometry/mirrored?
                                                  :heraldry.charge.geometry/reversed?]))
