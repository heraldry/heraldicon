(ns spec.heraldry.semy
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.semy/type #{:heraldry/semy})
(s/def :heraldry.semy/charge (su/spec :heraldry/charge))

(s/def :heraldry.semy/layout (s/nilable :heraldry/layout))
(s/def :heraldry.semy/rectangular? (s/nilable boolean?))
(s/def :heraldry.semy/manual-blazon (s/nilable string?))

(s/def :heraldry/semy (s/keys :req-un [:heraldry.semy/type
                                       :heraldry.semy/charge]
                              :opt-un [:heraldry.semy/layout
                                       :heraldry.semy/rectangular?
                                       :heraldry.semy/manual-blazon]))
