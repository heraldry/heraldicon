(ns spec.heraldry.ribbon
  (:require
   [cljs.spec.alpha :as s]
   [spec.heraldry.point]
   [spec.heraldry.ribbon.segment]))

(s/def :heraldry.ribbon/type #{:heraldry/ribbon})

(s/def :heraldry.ribbon/thickness number?)
(s/def :heraldry.ribbon/edge-angle number?)
(s/def :heraldry.ribbon/end-split number?)
(s/def :heraldry.ribbon/outline? boolean?)
(s/def :heraldry.ribbon/points (s/coll-of :heraldry/point :into []))
(s/def :heraldry.ribbon/segments (s/coll-of :heraldry.ribbon/segment :into []))

(s/def :heraldry/ribbon (s/keys :req-un [:heraldry.ribbon/type]
                                :opt-un [:heraldry.ribbon/thickness
                                         :heraldry.ribbon/edge-angle
                                         :heraldry.ribbon/end-split
                                         :heraldry.ribbon/outline?
                                         :heraldry.ribbon/points
                                         :heraldry.ribbon/segments]))
