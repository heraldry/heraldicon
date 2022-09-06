(ns spec.heraldicon.entity.attribution
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.entity.attribution :as attribution]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldicon.entity.attribution/nature (su/key-in? attribution/nature-map))
(s/def :heraldicon.entity.attribution/license (su/key-in? attribution/license-map))
(s/def :heraldicon.entity.attribution/license-version (su/key-in? attribution/cc-license-version-map))
(s/def :heraldicon.entity.attribution/source-name string?)
(s/def :heraldicon.entity.attribution/source-link string?)
(s/def :heraldicon.entity.attribution/source-license (su/key-in? attribution/license-map))
(s/def :heraldicon.entity.attribution/source-license-version (su/key-in? attribution/cc-license-version-map))
(s/def :heraldicon.entity.attribution/source-creator-name string?)
(s/def :heraldicon.entity.attribution/source-creator-link string?)
(s/def :heraldicon.entity.attribution/source-modification string?)

(s/def :heraldicon.entity/attribution (s/keys :opt-un [:heraldicon.entity.attribution/nature
                                                       :heraldicon.entity.attribution/license
                                                       :heraldicon.entity.attribution/license-version
                                                       :heraldicon.entity.attribution/source-license-version
                                                       :heraldicon.entity.attribution/source-modification
                                                       :heraldicon.entity.attribution/source-name
                                                       :heraldicon.entity.attribution/source-link
                                                       :heraldicon.entity.attribution/source-license
                                                       :heraldicon.entity.attribution/source-creator-name
                                                       :heraldicon.entity.attribution/source-creator-link]))
