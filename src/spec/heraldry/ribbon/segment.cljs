(ns spec.heraldry.ribbon.segment
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.font :as font]
   [heraldicon.heraldry.ribbon :as ribbon]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.point]))

(s/def :heraldry.ribbon.segment/type (su/key-in? ribbon/segment-type-map))

(s/def :heraldry.ribbon.segment/offset-x number?)
(s/def :heraldry.ribbon.segment/offset-y number?)
(s/def :heraldry.ribbon.segment/font-scale number?)
(s/def :heraldry.ribbon.segment/spacing number?)
(s/def :heraldry.ribbon.segment/text string?)
(s/def :heraldry.ribbon.segment/font (su/key-in? font/choice-map))

(s/def :heraldry.ribbon/segment (s/keys :req-un [:heraldry.ribbon.segment/type]
                                        :opt-un [:heraldry.ribbon.segment/offset-x
                                                 :heraldry.ribbon.segment/offset-y
                                                 :heraldry.ribbon.segment/font-scale
                                                 :heraldry.ribbon.segment/spacing
                                                 :heraldry.ribbon.segment/text
                                                 :heraldry.ribbon.segment/font]))
