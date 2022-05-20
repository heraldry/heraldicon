(ns spec.heraldry.semy
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.charge]
   [spec.heraldry.layout]))

(s/def :heraldry.semy/type #{:heraldry/semy})
(s/def :heraldry.semy/charge :heraldry/charge)

(s/def :heraldry.semy/layout :heraldry/layout)
(s/def :heraldry.semy/rectangular? boolean?)
(s/def :heraldry.semy/manual-blazon string?)

(s/def :heraldry/semy (s/keys :req-un [:heraldry.semy/type
                                       :heraldry.semy/charge]
                              :opt-un [:heraldry.semy/layout
                                       :heraldry.semy/rectangular?
                                       :heraldry.semy/manual-blazon]))
