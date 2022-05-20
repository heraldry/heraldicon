(ns spec.heraldry.field
  (:require
   [cljs.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]
   [heraldicon.heraldry.field.options :as field.options]
   [heraldicon.heraldry.field.type.per-pile :as per-pile]
   [heraldicon.heraldry.field.type.potenty :as potenty]
   [heraldicon.heraldry.field.type.vairy :as vairy]
   [heraldicon.heraldry.tincture :as tincture]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldicon.test-util :as tu]
   [spec.heraldry.charge]
   [spec.heraldry.charge-group]
   [spec.heraldry.layout]
   [spec.heraldry.line]
   [spec.heraldry.ordinary]
   [spec.heraldry.position]
   [spec.heraldry.semy]))

(s/def :heraldry.field/type (su/key-in? field.options/field-map))

(s/def :heraldry.field/tincture (s/or :none #{:none}
                                      :tincture (su/key-in? tincture/tincture-map)))

(s/def :heraldry.field/layout (s/nilable :heraldry/layout))

(s/def :heraldry.field.geometry/size-mode (s/nilable (su/key-in? per-pile/size-mode-map)))
(s/def :heraldry.field.geometry/size (s/nilable number?))
(s/def :heraldry.field.geometry/stretch (s/nilable number?))
(s/def :heraldry.field/geometry (s/nilable (s/keys :opt-un [:heraldry.field.geometry/size-mode
                                                            :heraldry.field.geometry/size
                                                            :heraldry.field.geometry/stretch])))
(s/def :heraldry.field.ref/type #{:heraldry.field.type/ref})
(s/def :heraldry.field.ref/index su/pos-number?)

(s/def :heraldry.field/subfield-or-ref (s/or :subfield :heraldry/field
                                             :ref (s/keys :req-un [:heraldry.field.ref/type
                                                                   :heraldry.field.ref/index])))

(s/def :heraldry.field/fields (s/coll-of :heraldry.field/subfield-or-ref :into []))
(s/def :heraldry.field/outline? (s/nilable boolean?))
(s/def :heraldry.field/inherit-environment? (s/nilable boolean?))
(s/def :heraldry.field/components (s/coll-of (s/or :ordinary :heraldry/ordinary
                                                   :charge :heraldry/charge
                                                   :charge-group :heraldry/charge-group
                                                   :semy :heraldry/semy)
                                             :into []))

(s/def :heraldry.field/line (s/nilable :heraldry/line))
(s/def :heraldry.field/opposite-line (s/nilable :heraldry/line))
(s/def :heraldry.field/extra-line (s/nilable :heraldry/line))

(s/def :heraldry.field/anchor (s/nilable :heraldry/position))
(s/def :heraldry.field/orientation (s/nilable :heraldry/position))
(s/def :heraldry.field/origin (s/nilable :heraldry/position))

(s/def :heraldry.field/thickness (s/nilable number?))
(s/def :heraldry.field/gap (s/nilable number?))
;; TODO: multi-spec based on type
(s/def :heraldry.field/variant (s/nilable (s/or :potenty (su/key-in? potenty/variant-map)
                                                :vairy (su/key-in? vairy/variant-map))))

(s/def :heraldry.field/manual-blazon (s/nilable string?))

(defmulti field-type (fn [field]
                       (if (= ((some-fn :heraldry.field/type :type) field)
                              :heraldry.field.type/plain)
                         :plain
                         :division)))

(defmethod field-type :plain [_]
  (s/keys :req-un [:heraldry.field/tincture]))

(defmethod field-type :division [_]
  (s/keys :req-un [:heraldry.field/fields]))

(s/def :heraldry/field (s/with-gen
                         (s/and (s/keys :req-un [:heraldry.field/type])
                                (s/multi-spec field-type :field-type)
                                (s/keys :opt-un [:heraldry.field/inherit-environment?
                                                 :heraldry.field/components
                                                 :heraldry.field/line
                                                 :heraldry.field/opposite-line
                                                 :heraldry.field/extra-line
                                                 :heraldry.field/layout
                                                 :heraldry.field/anchor
                                                 :heraldry.field/orientation
                                                 :heraldry.field/origin
                                                 :heraldry.field/outline?
                                                 :heraldry.field/geometry
                                                 :heraldry.field/thickness
                                                 :heraldry.field/gap
                                                 :heraldry.field/variant
                                                 :heraldry.field/manual-blazon]))
                         #(g/return {:type :heraldry.field.type/plain
                                     :tincture :azure})))
