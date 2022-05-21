(ns spec.heraldry.achievement
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.coat-of-arms]
   [spec.heraldry.helms]
   [spec.heraldry.render-options]))

(s/def :heraldry.achievement/type #{:heraldry/achievement})
(s/def :heraldry.achievement/coat-of-arms :heraldry/coat-of-arms)

(s/def :heraldry.achievement/render-options :heraldry/render-options)

(s/def :heraldry/achievement (s/keys :req-un [:heraldry.achievement/type
                                              :heraldry.achievement/coat-of-arms
                                              :heraldry/render-options
                                              :heraldry/helms
                                              #_:heraldry/ornaments]))
