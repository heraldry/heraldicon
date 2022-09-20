(ns spec.heraldry.position
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.field.type.per-pile :as per-pile]
   [heraldicon.heraldry.option.position :as position]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.position/point (su/key-in? position/orientation-point-map))
(s/def :heraldry.position/offset-x number?)
(s/def :heraldry.position/offset-y number?)
(s/def :heraldry.position/spacing-bottom number?)
(s/def :heraldry.position/spacing-left number?)
(s/def :heraldry.position/alignment (su/key-in? position/alignment-map))
(s/def :heraldry.position/type (su/key-in? per-pile/orientation-type-map))

(s/def :heraldry/position (s/keys :opt-un [:heraldry.position/point
                                           :heraldry.position/offset-x
                                           :heraldry.position/offset-y
                                           :heraldry.position/spacing-bottom
                                           :heraldry.position/spacing-left
                                           :heraldry.position/alignment
                                           :heraldry.position/type]))
