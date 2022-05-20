(ns spec.heraldry.ordinary.cottise
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.ordinary.cottise/type #{:heraldry/cottise})
(s/def :heraldry.ordinary.cottise/field (su/spec :heraldry/field))

(s/def :heraldry.ordinary.cottise/line (s/nilable :heraldry/line))
(s/def :heraldry.ordinary.cottise/opposite-line (s/nilable :heraldry/line))
(s/def :heraldry.ordinary.cottise/distance (s/nilable number?))
(s/def :heraldry.ordinary.cottise/thickness (s/nilable number?))
(s/def :heraldry.ordinary.cottise/outline? (s/nilable boolean?))

(s/def :heraldry.ordinary/cottise (s/keys :req-un [:heraldry.ordinary.cottise/type
                                                   :heraldry.ordinary.cottise/field]
                                          :opt-un [:heraldry.ordinary.cottise/line
                                                   :heraldry.ordinary.cottise/opposite-line
                                                   :heraldry.ordinary.cottise/distance
                                                   :heraldry.ordinary.cottise/thickness
                                                   :heraldry.ordinary.cottise/outline?]))
