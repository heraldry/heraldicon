(ns spec.heraldry.motto
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.motto :as motto]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.motto.geometry]
   [spec.heraldry.motto.ribbon-variant]
   [spec.heraldry.ribbon]))

(s/def :heraldry.motto/type (su/key-in? motto/type-map))

(s/def :heraldry.motto/anchor :heraldry/position)
(s/def :heraldry.motto/tincture-foreground (su/key-in? motto/tinctures-without-furs-map))
(s/def :heraldry.motto/tincture-background (su/key-in? (assoc motto/tinctures-without-furs-map :none true)))
(s/def :heraldry.motto/tincture-text (su/key-in? motto/tinctures-without-furs-map))
(s/def :heraldry.motto/ribbon :heraldry/ribbon)

(s/def :heraldry/motto (s/keys :req-un [:heraldry.motto/type]
                               :opt-un [:heraldry.motto/anchor
                                        :heraldry.motto/geometry
                                        :heraldry.motto/ribbon-variant
                                        :heraldry.motto/tincture-foreground
                                        :heraldry.motto/tincture-background
                                        :heraldry.motto/tincture-text
                                        :heraldry.motto/ribbon]))
