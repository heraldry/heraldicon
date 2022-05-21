(ns spec.heraldicon.entity.collection.element
  (:require
   [cljs.spec.alpha :as s]))

(s/def :heraldicon.entity.collection.element/type #{:heraldicon.entity.collection/element})

(s/def :heraldicon.entity.collection.element/name string?)

(s/def :heraldicon.entity.collection.element.reference/id string?)
(s/def :heraldicon.entity.collection.element.reference/version number?)
(s/def :heraldicon.entity.collection.element/reference
  (s/keys :req-un [:heraldicon.entity.collection.element.reference/id
                   :heraldicon.entity.collection.element.reference/version]))

(s/def :heraldicon.entity.collection/element (s/keys :req-un [:heraldicon.entity.collection.element/type]
                                                     :opt-un [:heraldicon.entity.collection.element/name
                                                              :heraldicon.entity.collection.element/reference]))
