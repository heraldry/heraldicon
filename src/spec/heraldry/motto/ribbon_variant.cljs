(ns spec.heraldry.motto.ribbon-variant
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldry.motto.ribbon-variant/id string?)
(s/def :heraldry.motto.ribbon-variant/version number?)
(s/def :heraldry.motto/ribbon-variant (s/keys :req-un [:heraldry.motto.ribbon-variant/id
                                                       :heraldry.motto.ribbon-variant/version]))
