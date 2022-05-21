(ns spec.heraldry.point
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.point/x number?)
(s/def :heraldry.point/y number?)

(s/def :heraldry/point (s/keys :req-un [:heraldry.point/x
                                        :heraldry.point/y]))
