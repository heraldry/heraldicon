(ns spec.heraldry.layout
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.layout/num-base-fields number?)
(s/def :heraldry.layout/num-fields-x number?)
(s/def :heraldry.layout/num-fields-y number?)
(s/def :heraldry.layout/offset-x number?)
(s/def :heraldry.layout/offset-y number?)
(s/def :heraldry.layout/stretch-x number?)
(s/def :heraldry.layout/stretch-y number?)
(s/def :heraldry.layout/rotation number?)
(s/def :heraldry/layout (s/keys :opt-un [:heraldry.layout/num-base-fields
                                         :heraldry.layout/num-fields-x
                                         :heraldry.layout/num-fields-y
                                         :heraldry.layout/offset-x
                                         :heraldry.layout/offset-y
                                         :heraldry.layout/stretch-x
                                         :heraldry.layout/stretch-y
                                         :heraldry.layout/rotation]))
