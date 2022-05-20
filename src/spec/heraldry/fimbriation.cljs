(ns spec.heraldry.fimbriation
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.tincture :as tincture]
   [spec.heraldicon.spec-util :as su]))

(s/def :heraldry.fimbriation/mode (s/nilable (su/key-in? fimbriation/mode-map)))
(s/def :heraldry.fimbriation/alignment (s/nilable (su/key-in? fimbriation/alignment-map)))
(s/def :heraldry.fimbriation/corner (s/nilable (su/key-in? fimbriation/corner-map)))
(s/def :heraldry.fimbriation/thickness-1 (s/nilable number?))
(s/def :heraldry.fimbriation/thickness-2 (s/nilable number?))

(def tincture
  (s/or :none #{:none}
        :tincture (su/key-in? tincture/tincture-map)))

(s/def :heraldry.fimbriation/tincture-1 tincture)
(s/def :heraldry.fimbriation/tincture-2 tincture)

(s/def :heraldry/fimbriation (s/nilable (s/keys :opt-un [:heraldry.fimbriation/mode
                                                         :heraldry.fimbriation/alignment
                                                         :heraldry.fimbriation/corner
                                                         :heraldry.fimbriation/thickness-1
                                                         :heraldry.fimbriation/thickness-2
                                                         :heraldry.fimbriation/tincture-1
                                                         :heraldry.fimbriation/tincture-2])))
