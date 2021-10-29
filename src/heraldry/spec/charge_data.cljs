(ns heraldry.spec.charge-data
  (:require
   [cljs.spec.alpha :as s]
   [heraldry.coat-of-arms.attributes :as attributes]
   [heraldry.coat-of-arms.tincture.core :as tincture]))

(s/def :heraldry.charge-data.data.edn-data/width number?)
(s/def :heraldry.charge-data.data.edn-data/height number?)
(s/def :heraldry.charge-data.data/edn-data (s/keys :req-un [:heraldry.charge-data.data.edn-data/width
                                                            :heraldry.charge-data.data.edn-data/height
                                                            :heraldry.charge-data.data.edn-data/data]))
(s/def :heraldry.charge-data/data (s/keys :req-un [:heraldry.charge-data.data/edn-data]))

(s/def :heraldry.charge-data/name #(and (string? %)
                                        (-> % count (> 0))))
(s/def :heraldry.charge-data/type keyword?)
(s/def :heraldry.charge-data/attitude attributes/attitude-map)
(s/def :heraldry.charge-data/facing attributes/facing-map)
(s/def :heraldry.charge-data/attributes #(every? (fn [[k v]]
                                                   (and (get attributes/attribute-map k)
                                                        (boolean? v))) %))
(s/def :heraldry.charge-data.colour/modifier attributes/tincture-modifier-for-charge-map)
(s/def :heraldry.charge-data.colour/qualifier attributes/tincture-modifier-qualifier-for-charge-map)
(s/def :heraldry.charge-data/colours #(every? (fn [[k v]]
                                                (and (string? k)
                                                     (re-matches #"^#[a-z0-9]{6}$" k)
                                                     (or (s/valid? :heraldry.charge-data.colour/modifier v)
                                                         (and (vector? v)
                                                              (s/valid? :heraldry.charge-data.colour/modifier (first v))
                                                              (s/valid? :heraldry.charge-data.colour/qualifier (second v)))))) %))
(s/def :heraldry.charge-data/spec-version number?)
(s/def :heraldry.charge-data/fixed-tincture tincture/fixed-tincture-map)
(s/def :heraldry/charge-data (s/keys :req-un [:heraldry.charge-data/name
                                              :heraldry.charge-data/type
                                              :heraldry.charge-data/data
                                              :heraldry.charge-data/spec-version]
                                     :opt-un [:heraldry.charge-data/attitude
                                              :heraldry.charge-data/facing
                                              :heraldry.charge-data/attributes
                                              :heraldry.charge-data/colours
                                              :heraldry.charge-data/fixed-tincture]))
