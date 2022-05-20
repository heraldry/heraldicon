(ns spec.heraldry.layout
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.layout/num-base-fields (s/nilable number?))
(s/def :heraldry.layout/num-fields-x (s/nilable number?))
(s/def :heraldry.layout/num-fields-y (s/nilable number?))
(s/def :heraldry.layout/offset-x (s/nilable number?))
(s/def :heraldry.layout/offset-y (s/nilable number?))
(s/def :heraldry.layout/stretch-x (s/nilable number?))
(s/def :heraldry.layout/stretch-y (s/nilable number?))
(s/def :heraldry.layout/rotation (s/nilable number?))
(s/def :heraldry/layout (s/keys :opt-un [:heraldry.layout/num-base-fields
                                         :heraldry.layout/num-fields-x
                                         :heraldry.layout/num-fields-y
                                         :heraldry.layout/offset-x
                                         :heraldry.layout/offset-y
                                         :heraldry.layout/stretch-x
                                         :heraldry.layout/stretch-y
                                         :heraldry.layout/rotation]))
