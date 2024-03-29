(ns spec.heraldry.charge-group
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.charge-group.options :as charge-group.options]
   [spec.heraldicon.spec-util :as su]
   [spec.heraldry.charge]
   [spec.heraldry.position]))

(s/def :heraldry.charge-group/type (su/key-in? charge-group.options/type-map))
(s/def :heraldry.charge-group/charges (s/coll-of :heraldry/charge :into []))

(s/def :heraldry.charge-group/anchor :heraldry/position)
(s/def :heraldry.charge-group/manual-blazon string?)

(s/def :heraldry.charge-group/spacing number?)
(s/def :heraldry.charge-group/stretch number?)
(s/def :heraldry.charge-group/strip-angle number?)

(s/def :heraldry.charge-group/start-angle number?)
(s/def :heraldry.charge-group/arc-angle number?)
(s/def :heraldry.charge-group/slots (s/coll-of (s/nilable number?) :into []))
(s/def :heraldry.charge-group/radius number?)
(s/def :heraldry.charge-group/arc-stretch number?)
(s/def :heraldry.charge-group/rotate-charges? boolean?)

(s/def :heraldry.charge-group/distance number?)
(s/def :heraldry.charge-group/offset number?)

(s/def :heraldry.charge-group.element/type #{:heraldry.charge-group.element.type/strip})

(s/def :heraldry.charge-group.strip/slots (s/coll-of (s/nilable number?) :into []))
(s/def :heraldry.charge-group.strip/stretch number?)
(s/def :heraldry.charge-group.strip/offset number?)
(s/def :heraldry.charge-group/strip (s/keys :req-un [:heraldry.charge-group.element/type]
                                            :opt-un [:heraldry.charge-group.strip/slots
                                                     :heraldry.charge-group.strip/stretch
                                                     :heraldry.charge-group.strip/offset]))

(s/def :heraldry.charge-group/strips (s/coll-of :heraldry.charge-group/strip :into []))

(defmulti charge-group-type (some-fn :heraldry.charge-group/type :type))

(defmethod charge-group-type :heraldry.charge-group.type/rows [_]
  (s/keys :opt-un [:heraldry.charge-group/anchor
                   :heraldry.charge-group/spacing
                   :heraldry.charge-group/stretch
                   :heraldry.charge-group/strip-angle
                   :heraldry.charge-group/strips]))

(defmethod charge-group-type :heraldry.charge-group.type/columns [_]
  (s/keys :opt-un [:heraldry.charge-group/anchor
                   :heraldry.charge-group/spacing
                   :heraldry.charge-group/stretch
                   :heraldry.charge-group/strip-angle
                   :heraldry.charge-group/strips]))

(defmethod charge-group-type :heraldry.charge-group.type/arc [_]
  (s/keys :opt-un [:heraldry.charge-group/anchor
                   :heraldry.charge-group/start-angle
                   :heraldry.charge-group/arc-angle
                   :heraldry.charge-group/slots
                   :heraldry.charge-group/radius
                   :heraldry.charge-group/arc-stretch
                   :heraldry.charge-group/rotate-charges?]))

(defmethod charge-group-type :heraldry.charge-group.type/in-orle [_]
  (s/keys :opt-un [:heraldry.charge-group/distance
                   :heraldry.charge-group/offset
                   :heraldry.charge-group/slots]))

(s/def :heraldry/charge-group (s/and (s/keys :req-un [:heraldry.charge-group/type
                                                      :heraldry.charge-group/charges]
                                             :opt-un [:heraldry.charge-group/manual-blazon])
                                     (s/multi-spec charge-group-type :charge-group-type)))
