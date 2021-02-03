(ns heraldry.spec.charge-data
  (:require [cljs.spec.alpha :as s]
            [heraldry.coat-of-arms.attributes :as attributes]))

(s/def :heraldry.charge-data.data.edn-data/width number?)
(s/def :heraldry.charge-data.data.edn-data/height number?)
(s/def :heraldry.charge-data.data.edn-data.colour/type #{:primary
                                                         :outline
                                                         :keep
                                                         :eyes-and-teeth
                                                         :armed
                                                         :langued
                                                         :attired
                                                         :unguled
                                                         :beaked})
(s/def :heraldry.charge-data.data.edn-data/colours #(every? (fn [k v]
                                                              (and (re-matches #"^#[a-z0-9]{6}$" k)
                                                                   (s/valid? :heraldry.charge-data.colour/type v))) %))
(s/def :heraldry.charge-data.data/edn-data (s/keys :req-un [:heraldry.charge-data.data.edn-data/width
                                                            :heraldry.charge-data.data.edn-data/height
                                                            :heraldry.charge-data.data.edn-data/data]))
(s/def :heraldry.charge-data/data (s/keys :req-un [:heraldry.charge-data.data/edn-data]))

(s/def :heraldry.charge-data/name #(and (string? %)
                                        (-> % count (> 0))))
(s/def :heraldry.charge-data/type keyword?)
(s/def :heraldry.charge-data/attitude attributes/attitude-map)
(s/def :heraldry.charge-data/facing #{:none
                                      :to-dexter
                                      :to-sinister
                                      :affronte
                                      :en-arriere
                                      :guardant
                                      :reguardant
                                      :salient
                                      :in-trian-aspect})
(s/def :heraldry.charge-data/attributes #(every? (fn [[k v]]
                                                   (and (get #{:coward
                                                               :pierced
                                                               :voided} k)
                                                        (boolean? v))) %))
(s/def :heraldry.charge-data/spec-version number?)
(s/def :heraldry/charge-data (s/keys :req-un [:heraldry.charge-data/name
                                              :heraldry.charge-data/type
                                              :heraldry.charge-data/data
                                              :heraldry.charge-data/spec-version]
                                     :opt-un [:heraldry.charge-data/attitude
                                              :heraldry.charge-data/facing
                                              :heraldry.charge-data/attributes]))
