(ns spec.heraldry.ordinary.geometry
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.ordinary.type.pile :as pile]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.ordinary.geometry/size-mode (s/nilable (su/key-in? pile/size-mode-map)))
(s/def :heraldry.ordinary.geometry/size (s/nilable number?))
(s/def :heraldry.ordinary.geometry/stretch (s/nilable number?))
(s/def :heraldry.ordinary.geometry/width (s/nilable number?))
(s/def :heraldry.ordinary.geometry/height (s/nilable number?))
(s/def :heraldry.ordinary.geometry/thickness (s/nilable number?))
(s/def :heraldry.ordinary.geometry/eccentricity (s/nilable number?))

(s/def :heraldry.ordinary/geometry (s/nilable (s/keys :opt-un [:heraldry.field.geometry/size-mode
                                                               :heraldry.field.geometry/size
                                                               :heraldry.field.geometry/stretch
                                                               :heraldry.field.geometry/width
                                                               :heraldry.field.geometry/height
                                                               :heraldry.field.geometry/eccentricity
                                                               :heraldry.field.geometry/thickness])))
