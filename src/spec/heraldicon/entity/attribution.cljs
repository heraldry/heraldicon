(ns spec.heraldicon.entity.attribution
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.entity.attribution :as attribution]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldicon.entity.attribution/nature (su/key-in? attribution/nature-map))
(s/def :heraldicon.entity.attribution/license (su/key-in? attribution/license-map))
(s/def :heraldicon.entity.attribution/license-version (su/key-in? attribution/cc-license-version-map))
(s/def :heraldicon.entity.attribution/source-name su/non-blank-string?)
(s/def :heraldicon.entity.attribution/source-link su/non-blank-string?)
(s/def :heraldicon.entity.attribution/source-license (su/key-in? attribution/license-map))
(s/def :heraldicon.entity.attribution/source-license-version (su/key-in? attribution/cc-license-version-map))
(s/def :heraldicon.entity.attribution/source-creator-name su/non-blank-string?)
(s/def :heraldicon.entity.attribution/source-creator-link su/non-blank-string?)
(s/def :heraldicon.entity.attribution/source-modification string?)

(defmulti attribution-type #(-> % :nature (or :own-work)))

(defmethod attribution-type :own-work [_]
  (s/keys :opt-un [:heraldicon.entity.attribution/nature
                   :heraldicon.entity.attribution/license
                   :heraldicon.entity.attribution/license-version]))

(defmethod attribution-type :derivative [_]
  (s/keys :opt-un [:heraldicon.entity.attribution/nature
                   :heraldicon.entity.attribution/license
                   :heraldicon.entity.attribution/license-version
                   :heraldicon.entity.attribution/source-license-version
                   :heraldicon.entity.attribution/source-modification]
          :req-un [:heraldicon.entity.attribution/source-name
                   :heraldicon.entity.attribution/source-link
                   :heraldicon.entity.attribution/source-license
                   :heraldicon.entity.attribution/source-creator-name
                   :heraldicon.entity.attribution/source-creator-link]))

(s/def :heraldicon.entity/attribution (s/multi-spec attribution-type :attribution-type))
