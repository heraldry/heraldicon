(ns spec.heraldry.line
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.line.core :as line]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.line/type (s/nilable (su/key-in? line/line-map)))

(s/def :heraldry.line/eccentricity (s/nilable number?))
(s/def :heraldry.line/height (s/nilable number?))
(s/def :heraldry.line/width (s/nilable number?))
(s/def :heraldry.line/offset (s/nilable number?))
(s/def :heraldry.line/spacing (s/nilable number?))
(s/def :heraldry.line/base-line (s/nilable (su/key-in? line/base-line-map)))
(s/def :heraldry.line/corner-dampening-radius (s/nilable number?))
(s/def :heraldry.line/corner-dampening-mode (s/nilable (su/key-in? line/corner-dampening-mode-map)))
(s/def :heraldry.line/flipped? (s/nilable boolean?))
(s/def :heraldry.line/mirrored? (s/nilable boolean?))
(s/def :heraldry.line/fimbriation (s/nilable :heraldry/fimbriation))

(s/def :heraldry/line (s/nilable (s/keys :opt-un [:heraldry.line/type
                                                  :heraldry.line/eccentricity
                                                  :heraldry.line/height
                                                  :heraldry.line/width
                                                  :heraldry.line/spacing
                                                  :heraldry.line/offset
                                                  :heraldry.line/mirrored?
                                                  :heraldry.line/flipped?
                                                  :heraldry.line/fimbriation])))
