(ns spec.heraldry.ordinary.cottise
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.line]))

(s/def :heraldry.ordinary.cottise/type #{:heraldry/cottise})
(s/def :heraldry.ordinary.cottise/field (su/spec :heraldry/field))

(s/def :heraldry.ordinary.cottise/line :heraldry/line)
(s/def :heraldry.ordinary.cottise/opposite-line :heraldry/line)
(s/def :heraldry.ordinary.cottise/distance number?)
(s/def :heraldry.ordinary.cottise/thickness number?)
(s/def :heraldry.ordinary.cottise/outline? boolean?)

(s/def :heraldry.ordinary/cottise (s/keys :req-un [:heraldry.ordinary.cottise/type
                                                   :heraldry.ordinary.cottise/field]
                                          :opt-un [:heraldry.ordinary.cottise/line
                                                   :heraldry.ordinary.cottise/opposite-line
                                                   :heraldry.ordinary.cottise/distance
                                                   :heraldry.ordinary.cottise/thickness
                                                   :heraldry.ordinary.cottise/outline?]))
