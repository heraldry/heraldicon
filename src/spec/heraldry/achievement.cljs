(ns spec.heraldry.achievement
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.coat-of-arms]
   [spec.heraldry.render-options]))

(s/def :heraldry.achievement/type #{:heraldry/achievement})

(s/def :heraldry/achievement (s/keys :req-un [:heraldry.achievement/type
                                              #_:heraldry/coat-of-arms]
                                     #_#_:opt-un [:heraldry/render-options
                                                  :heraldry/helms
                                                  :heraldry/ornaments]))
