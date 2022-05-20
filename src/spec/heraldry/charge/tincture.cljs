(ns spec.heraldry.charge.tincture
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.tincture :as tincture]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.charge.tincture.key/shading #{:shadow :highlight})
(s/def :heraldry.charge.tincture.key/extra #{:eyed :toothed})
(s/def :heraldry.charge.tincture/key (s/or :shading :heraldry.charge.tincture.key/shading
                                           :extra :heraldry.charge.tincture.key/extra
                                           :modifier (su/key-in? attributes/applicable-tincture-modifier-map)))
(s/def :heraldry.charge.tincture.value/shading number?)
(s/def :heraldry.charge.tincture.value/tincture (su/key-in? tincture/tincture-map))
(s/def :heraldry.charge.tincture/value (s/or :tincture :heraldry.charge.tincture.value/tincture
                                             :shading :heraldry.charge.tincture.value/shading))

(s/def :heraldry.charge/tincture (s/and (s/map-of :heraldry.charge.tincture/key
                                                  :heraldry.charge.tincture/value)
                                        (s/every (fn [[k [category _]]]
                                                   (if (s/valid? :heraldry.charge.tincture.key/shading k)
                                                     (= category :shading)
                                                     (= category :tincture))))))
