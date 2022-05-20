(ns spec.heraldry.line
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.line.core :as line]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.fimbriation]))

(s/def :heraldry.line/type (su/key-in? line/line-map))

(s/def :heraldry.line/eccentricity number?)
(s/def :heraldry.line/height number?)
(s/def :heraldry.line/width number?)
(s/def :heraldry.line/offset number?)
(s/def :heraldry.line/spacing number?)
(s/def :heraldry.line/base-line (su/key-in? line/base-line-map))
(s/def :heraldry.line/corner-dampening-radius number?)
(s/def :heraldry.line/corner-dampening-mode (su/key-in? line/corner-dampening-mode-map))
(s/def :heraldry.line/flipped? boolean?)
(s/def :heraldry.line/mirrored? boolean?)
(s/def :heraldry.line/fimbriation :heraldry/fimbriation)

(s/def :heraldry/line (s/keys :opt-un [:heraldry.line/type
                                       :heraldry.line/eccentricity
                                       :heraldry.line/height
                                       :heraldry.line/width
                                       :heraldry.line/spacing
                                       :heraldry.line/base-line
                                       :heraldry.line/corner-dampening-radius
                                       :heraldry.line/corner-dampening-mode
                                       :heraldry.line/offset
                                       :heraldry.line/mirrored?
                                       :heraldry.line/flipped?
                                       :heraldry.line/fimbriation]))
