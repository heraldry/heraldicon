(ns spec.heraldry.position
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.field.type.per-pile :as per-pile]
   [heraldicon.heraldry.option.position :as position]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.position/point (s/nilable (su/key-in? position/orientation-point-map)))
(s/def :heraldry.position/offset-x (s/nilable number?))
(s/def :heraldry.position/offset-y (s/nilable number?))
(s/def :heraldry.position/alignment (s/nilable (su/key-in? position/alignment-map)))
(s/def :heraldry.position/type (s/nilable (su/key-in? per-pile/orientation-type-map)))

(s/def :heraldry/position (s/keys :opt-un [:heraldry.position/point
                                           :heraldry.position/offset-x
                                           :heraldry.position/offset-y
                                           :heraldry.position/alignment
                                           :heraldry.position/type]))
