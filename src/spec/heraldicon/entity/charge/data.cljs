(ns spec.heraldicon.entity.charge.data
  (:require
   [cljs.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.tincture :as tincture]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldicon.entity.charge.data/type #{:heraldicon.entity.charge/data})

(s/def :heraldicon.entity.charge.data.edn-data/width number?)
(s/def :heraldicon.entity.charge.data.edn-data/height number?)
(s/def :heraldicon.entity.charge.data.edn-data/data vector?)
(s/def :heraldicon.entity.charge.data/edn-data
  (s/keys :req-un [:heraldicon.entity.charge.data.edn-data/width
                   :heraldicon.entity.charge.data.edn-data/height
                   :heraldicon.entity.charge.data.edn-data/data]))

(s/def :heraldicon.entity.charge.data/charge-type su/non-blank-string?)
(s/def :heraldicon.entity.charge.data/landscape? boolean?)
(s/def :heraldicon.entity.charge.data/attitude (su/key-in? attributes/attitude-map))
(s/def :heraldicon.entity.charge.data/facing (su/key-in? attributes/facing-map))
(s/def :heraldicon.entity.charge.data/fixed-tincture (su/key-in? tincture/fixed-tincture-map))
(s/def :heraldicon.entity.charge.data/attributes (s/map-of (su/key-in? attributes/attribute-map)
                                                           boolean?))

(s/def :heraldicon.entity.charge.data.colour/key (s/with-gen
                                                   (s/and string?
                                                          #(re-matches #"^#[a-fA-F0-9]{6}$" %))
                                                   #(g/return "#123456")))
(s/def :heraldicon.entity.charge.data.colour.value/modifier (su/key-in? attributes/tincture-modifier-for-charge-map))
(s/def :heraldicon.entity.charge.data.colour.value/qualifier (su/key-in? attributes/tincture-modifier-qualifier-for-charge-map))
(s/def :heraldicon.entity.charge.data.colour/value (s/or :modifier :heraldicon.entity.charge.data.colour.value/modifier
                                                         :vector (s/tuple :heraldicon.entity.charge.data.colour.value/modifier
                                                                          :heraldicon.entity.charge.data.colour.value/qualifier)))
(s/def :heraldicon.entity.charge.data/colours (s/map-of :heraldicon.entity.charge.data.colour/key
                                                        :heraldicon.entity.charge.data.colour/value))

(s/def :heraldicon.entity.charge/data (s/keys :req-un [:heraldicon.entity.charge.data/type
                                                       :heraldicon.entity.charge.data/charge-type]
                                              :opt-un [:heraldicon.entity.charge.data/edn-data
                                                       :heraldicon.entity.charge.data/landscape?
                                                       :heraldicon.entity.charge.data/attitude
                                                       :heraldicon.entity.charge.data/facing
                                                       :heraldicon.entity.charge.data/fixed-tincture
                                                       :heraldicon.entity.charge.data/attributes
                                                       :heraldicon.entity.charge.data/colours]))
