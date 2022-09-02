(ns spec.heraldry.subfield
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.subfield :as subfield]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.charge]
   [spec.heraldry.charge-group]
   [spec.heraldry.layout]
   [spec.heraldry.line]
   [spec.heraldry.ordinary]
   [spec.heraldry.position]
   [spec.heraldry.semy]))

(s/def :heraldry.subfield/type (su/key-in? subfield/subfield-map))

(s/def :heraldry.subfield/field (su/spec :heraldry/field))

(s/def :heraldry.subfield/index su/pos-number?)

(defmulti subfield-type (fn [field]
                          (let [type ((some-fn :heraldry.subfield/type :type) field)]
                            (case type
                              :heraldry.subfield.type/field :field
                              :heraldry.subfield.type/reference :reference))))

(defmethod subfield-type :field [_]
  (s/keys :req-un [:heraldry.subfield/field]))

(defmethod subfield-type :reference [_]
  (s/keys :req-un [:heraldry.subfield/index]))

(s/def :heraldry/subfield (s/and (s/keys :req-un [:heraldry.subfield/type])
                                 (s/multi-spec subfield-type :subfield-type)))
