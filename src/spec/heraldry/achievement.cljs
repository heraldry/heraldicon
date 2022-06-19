(ns spec.heraldry.achievement
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.coat-of-arms]
   [spec.heraldry.helms]
   [spec.heraldry.ornaments]
   [spec.heraldry.render-options]))

(s/def :heraldry.achievement/type #{:heraldry/achievement})
(s/def :heraldry.achievement/coat-of-arms :heraldry/coat-of-arms)

(s/def :heraldry.achievement/render-options :heraldry/render-options)
(s/def :heraldry.achievement/helms :heraldry/helms)
(s/def :heraldry.achievement/ornaments :heraldry/ornaments)

(s/def :heraldry/achievement (s/keys :req-un [:heraldry.achievement/type
                                              :heraldry.achievement/coat-of-arms
                                              :heraldry.achievement/render-options
                                              :heraldry.achievement/helms
                                              :heraldry.achievement/ornaments]))
