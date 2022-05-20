(ns spec.heraldry.ordinary.geometry
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.ordinary.type.pile :as pile]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.ordinary.geometry/size-mode (su/key-in? pile/size-mode-map))
(s/def :heraldry.ordinary.geometry/size number?)
(s/def :heraldry.ordinary.geometry/stretch number?)
(s/def :heraldry.ordinary.geometry/width number?)
(s/def :heraldry.ordinary.geometry/height number?)
(s/def :heraldry.ordinary.geometry/thickness number?)
(s/def :heraldry.ordinary.geometry/eccentricity number?)

(s/def :heraldry.ordinary/geometry (s/keys :opt-un [:heraldry.ordinary.geometry/size-mode
                                                    :heraldry.ordinary.geometry/size
                                                    :heraldry.ordinary.geometry/stretch
                                                    :heraldry.ordinary.geometry/width
                                                    :heraldry.ordinary.geometry/height
                                                    :heraldry.ordinary.geometry/eccentricity
                                                    :heraldry.ordinary.geometry/thickness]))
