(ns spec.heraldry.helms
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.helm]))

(s/def :heraldry.helms/type #{:heraldry/helms})

(s/def :heraldry.helms/elements (s/coll-of :heraldry/helm :into []))

(s/def :heraldry/helms (s/keys :req-un [:heraldry.helms/type]
                               :opt-un [:heraldry.helms/elements]))
