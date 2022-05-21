(ns spec.heraldry.shield-separator
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.shield-separator/type #{:heraldry/shield-separator})

(s/def :heraldry/shield-separator (s/keys :req-un [:heraldry.shield-separator/type]))
