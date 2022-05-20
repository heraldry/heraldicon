(ns spec.heraldry.charge
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.option.attributes :as attributes]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.charge/type (s/and keyword?
                                    #(-> % namespace (= "heraldry.charge.type"))))
(s/def :heraldry.charge/field (su/spec :heraldry/field))

(s/def :heraldry.charge/anchor (s/nilable :heraldry/position))
(s/def :heraldry.charge/orientation (s/nilable :heraldry/position))

(s/def :heraldry.charge/attitude (s/nilable (su/key-in? attributes/attitude-map)))
(s/def :heraldry.charge/facing (s/nilable (su/key-in? attributes/facing-map)))

(s/def :heraldry.charge/fimbriation (s/nilable :heraldry/fimbriation))
(s/def :heraldry.charge/outline-mode (s/nilable (su/key-in? charge.shared/outline-mode-map)))
(s/def :heraldry.charge/vertical-mask (s/nilable number?))

(s/def :heraldry.charge/escutcheon (s/nilable (s/or :none #{:none}
                                                    :escutcheon (su/key-in? escutcheon/choice-map))))

(s/def :heraldry.charge/num-points (s/nilable number?))
(s/def :heraldry.charge/eccentricity (s/nilable number?))
(s/def :heraldry.charge/wavy-rays? (s/nilable boolean?))

(s/def :heraldry.charge/ignore-layer-separator? (s/nilable boolean?))

(s/def :heraldry.charge/manual-blazon (s/nilable string?))

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
