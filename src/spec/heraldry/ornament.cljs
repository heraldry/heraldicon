(ns spec.heraldry.ornament
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.charge]
   [spec.heraldry.charge-group]
   #_[spec.heraldry.motto]
   [spec.heraldry.shield-separator]))

(s/def :heraldry/ornament (s/or :charge :heraldry/charge
                                :charge-group :heraldry/charge-group
                                :shield-separator :heraldry/shield-separator))
