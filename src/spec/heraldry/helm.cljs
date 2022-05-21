(ns spec.heraldry.helm
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.charge]
   [spec.heraldry.shield-separator]))

(s/def :heraldry.helm/type #{:heraldry/helm})

(s/def :heraldry.helm/component (s/or :charge :heraldry/charge
                                      :shield-separator :heraldry/shield-separator))
(s/def :heraldry.helm/components (s/coll-of :heraldry.helm/component :into []))

(s/def :heraldry/helm (s/keys :req-un [:heraldry.helm/type]
                              :opt-un [:heraldry.helm/components]))
