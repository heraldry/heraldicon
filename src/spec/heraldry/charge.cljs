(ns spec.heraldry.charge
  (:require
   [cljs.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.option.attributes :as attributes]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.charge.geometry]
   [spec.heraldry.charge.tincture]
   [spec.heraldry.charge.variant]
   [spec.heraldry.fimbriation]
   [spec.heraldry.position]))

(s/def :heraldry.charge/type (s/with-gen
                               (s/and keyword?
                                      #(-> % namespace (= "heraldry.charge.type")))
                               #(g/return :heraldry.charge.type/lion)))
(s/def :heraldry.charge/field (su/spec :heraldry/field))

(s/def :heraldry.charge/anchor :heraldry/position)
(s/def :heraldry.charge/orientation :heraldry/position)

(s/def :heraldry.charge/attitude (su/key-in? attributes/attitude-map))
(s/def :heraldry.charge/facing (su/key-in? attributes/facing-map))

(s/def :heraldry.charge/fimbriation :heraldry/fimbriation)
(s/def :heraldry.charge/outline-mode (su/key-in? charge.shared/outline-mode-map))
(s/def :heraldry.charge/vertical-mask number?)

(s/def :heraldry.charge/escutcheon (s/or :none #{:none}
                                         :escutcheon (su/key-in? escutcheon/choice-map)))

(s/def :heraldry.charge/num-points number?)
(s/def :heraldry.charge/eccentricity number?)
(s/def :heraldry.charge/wavy-rays? boolean?)

(s/def :heraldry.charge/ignore-layer-separator? boolean?)

(s/def :heraldry.charge/manual-blazon string?)

(s/def :heraldry/charge (s/keys :req-un [:heraldry.charge/type
                                         :heraldry.charge/field]
                                :opt-un [:heraldry.charge/anchor
                                         :heraldry.charge/orientation
                                         :heraldry.charge/attitude
                                         :heraldry.charge/facing
                                         :heraldry.charge/geometry
                                         :heraldry.charge/fimbriation
                                         :heraldry.charge/outline-mode
                                         :heraldry.charge/vertical-mask
                                         :heraldry.charge/escutcheon
                                         :heraldry.charge/num-points
                                         :heraldry.charge/eccentricity
                                         :heraldry.charge/wavy-rays?
                                         :heraldry.charge/variant
                                         :heraldry.charge/ignore-layer-separator?
                                         :heraldry.charge/tincture
                                         :heraldry.charge/manual-blazon]))
