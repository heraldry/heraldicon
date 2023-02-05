(ns spec.heraldicon.entity.collection.data
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.font :as font]
   [spec.heraldicon.entity.collection.element]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldicon.entity.collection.data/type #{:heraldicon.entity.collection/data})

(s/def :heraldicon.entity.collection.data/num-columns number?)
(s/def :heraldicon.entity.collection.data/font (su/key-in? font/font-map))
(s/def :heraldicon.entity.collection.data/font-title (su/key-in? font/font-map))
(s/def :heraldicon.entity.collection.data/elements (s/coll-of :heraldicon.entity.collection/element :into []))

(s/def :heraldicon.entity.collection/data (s/keys :req-un [:heraldicon.entity.collection.data/type]
                                                  :opt-un [:heraldicon.entity.collection.data/num-columns
                                                           :heraldicon.entity.collection.data/font
                                                           :heraldicon.entity.collection.data/font-title
                                                           :heraldicon.entity.collection.data/elements]))
