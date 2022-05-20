(ns spec.heraldry.ordinary
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.heraldry.ordinary.type.label :as label]
   [heraldicon.heraldry.ordinary.type.point :as point]
   [heraldicon.heraldry.ordinary.type.quarter :as quarter]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.line]
   [spec.heraldry.ordinary.cottise]
   [spec.heraldry.position]))

(s/def :heraldry.ordinary/type (su/key-in? ordinary.options/ordinary-map))

(s/def :heraldry.ordinary/field (su/spec :heraldry/field))

(s/def :heraldry.ordinary/line (s/nilable :heraldry/line))
(s/def :heraldry.ordinary/opposite-line (s/nilable :heraldry/line))
(s/def :heraldry.ordinary/extra-line (s/nilable :heraldry/line))

(s/def :heraldry.ordinary/anchor (s/nilable :heraldry/position))
(s/def :heraldry.ordinary/orientation (s/nilable :heraldry/position))
(s/def :heraldry.ordinary/origin (s/nilable :heraldry/position))

(s/def :heraldry.ordinary/thickness (s/nilable number?))
(s/def :heraldry.ordinary/distance (s/nilable number?))
(s/def :heraldry.ordinary/corner-radius (s/nilable number?))
(s/def :heraldry.ordinary/smoothing (s/nilable number?))

(s/def :heraldrt.ordinary/variant (s/or :label (su/key-in? label/variant-map)
                                        :quarter (su/key-in? quarter/variant-map)
                                        :point (su/key-in? point/variant-map)))

(s/def :heraldry.ordinary/fimbriation (s/nilable :heraldry/fimbriation))

(s/def :heraldry.ordinary/cottise-1 (s/nilable :heraldry.ordinary/cottise))
(s/def :heraldry.ordinary/cottise-2 (s/nilable :heraldry.ordinary/cottise))
(s/def :heraldry.ordinary/cottise-opposite-1 (s/nilable :heraldry.ordinary/cottise))
(s/def :heraldry.ordinary/cottise-opposite-2 (s/nilable :heraldry.ordinary/cottise))
(s/def :heraldry.ordinary/cottise-extra-1 (s/nilable :heraldry.ordinary/cottise))
(s/def :heraldry.ordinary/cottise-extra-2 (s/nilable :heraldry.ordinary/cottise))

(s/def :heraldry.ordinary/cottising (s/nilable (s/keys :opt-un [:heraldry.ordinary/cottise-1
                                                                :heraldry.ordinary/cottise-2
                                                                :heraldry.ordinary/cottise-opposite-1
                                                                :heraldry.ordinary/cottise-opposite-2
                                                                :heraldry.ordinary/cottise-extra-1
                                                                :heraldry.ordinary/cottise-extra-2])))

(s/def :heraldry.ordinary/manual-blazon (s/nilable string?))

(s/def :heraldry/ordinary (s/keys :req-un [:heraldry.ordinary/type
                                           :heraldry.ordinary/field]
                                  :opt-un [:heraldry.ordinary/line
                                           :heraldry.ordinary/opposite-line
                                           :heraldry.ordinary/extra-line
                                           :heraldry.ordinary/anchor
                                           :heraldry.ordinary/orientation
                                           :heraldry.ordinary/origin
                                           :heraldry.ordinary/geometry
                                           :heraldry.ordinary/thickness
                                           :heraldry.ordinary/distance
                                           :heraldry.ordinary/corner-radius
                                           :heraldry.ordinary/num-points
                                           :heraldry.ordinary/variant
                                           :heraldry.ordinary/smoothing
                                           :heraldry.ordinary/fimbriation
                                           :heraldry.ordinary/cottising
                                           :heraldry.ordinary/manual-blazon]))
