(ns heraldicon.spec.coat-of-arms
  (:require
   [cljs.spec.alpha :as s]
   [heraldicon.heraldry.charge-group.options :as charge-group.options]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.render.texture :as texture]
   [heraldicon.render.theme :as theme]))

(s/def :heraldry/spec-version number?)
(s/def :heraldry/escutcheon #(get escutcheon/kinds-map %))
(s/def :heraldry/tincture #(or (= % :none)
                               (get tincture/tincture-map %)))

(s/def :heraldry.line/type (s/nilable line/line-map))
(s/def :heraldry.line/eccentricity (s/nilable number?))
(s/def :heraldry.line/height (s/nilable number?))
(s/def :heraldry.line/spacing (s/nilable number?))
(s/def :heraldry.line/offset (s/nilable number?))
(s/def :heraldry.line/width (s/nilable number?))
(s/def :heraldry.line/flipped? (s/nilable boolean?))
(s/def :heraldry.line/mirrored? (s/nilable boolean?))
(s/def :heraldry/line (s/nilable (s/keys :opt-un [:heraldry.line/type
                                                  :heraldry.line/eccentricity
                                                  :heraldry.line/height
                                                  :heraldry.line/width
                                                  :heraldry.line/spacing
                                                  :heraldry.line/offset
                                                  :heraldry.line/mirrored?
                                                  :heraldry.line/flipped?])))

(s/def :heraldry.position/point (s/nilable position/orientation-point-map))
(s/def :heraldry.position/offset-x (s/nilable number?))
(s/def :heraldry.position/offset-y (s/nilable number?))
(s/def :heraldry/position (s/nilable (s/keys :opt-un [:heraldry.position/point
                                                      :heraldry.position/offset-x
                                                      :heraldry.position/offset-y])))

(s/def :heraldry/anchor #(s/valid? :heraldry/position %))
(s/def :heraldry/orientation #(s/valid? :heraldry/position %))

(s/def :heraldry.geometry/size (s/nilable number?))
(s/def :heraldry/geometry (s/nilable (s/keys :opt-un [:heraldry.geometry/size])))

(s/def :heraldry.field.layout/num-base-fields (s/nilable number?))
(s/def :heraldry.field.layout/num-fields-x (s/nilable number?))
(s/def :heraldry.field.layout/num-fields-y (s/nilable number?))
(s/def :heraldry.field.layout/offset-x (s/nilable number?))
(s/def :heraldry.field.layout/offset-y (s/nilable number?))
(s/def :heraldry.field.layout/stretch-x (s/nilable number?))
(s/def :heraldry.field.layout/stretch-y (s/nilable number?))
(s/def :heraldry.field.layout/rotation (s/nilable number?))
(s/def :heraldry.field/layout (s/nilable (s/keys :opt-un [:heraldry.field.layout/num-base-fields
                                                          :heraldry.field.layout/num-fields-x
                                                          :heraldry.field.layout/num-fields-y
                                                          :heraldry.field.layout/offset-x
                                                          :heraldry.field.layout/offset-y
                                                          :heraldry.field.layout/stretch-x
                                                          :heraldry.field.layout/stretch-y
                                                          :heraldry.field.layout/rotation])))
(s/def :heraldry.field/fields (s/coll-of :heraldry/field :into []))
(s/def :heraldry.field/outline? (s/nilable boolean?))
(s/def :heraldry.field/inherit-environment? (s/nilable boolean?))
(s/def :heraldry.field/components (s/coll-of #(or (s/valid? :heraldry/ordinary %)
                                                  (s/valid? :heraldry/charge %)
                                                  (s/valid? :heraldry/charge-group %)
                                                  (s/valid? :heraldry/semy %)) :into []))

(s/def :heraldry.field.ref/index #(and (number? %)
                                       (>= % 0)))

(defmulti field-type (fn [field]
                       (let [type ((some-fn :heraldry.field/type :type) field)
                             type ;; strip the namespace, but only if it is the right one
                             (if (some-> type namespace (= "heraldry.field.type"))
                               (-> type name keyword)
                               type)]
                         (case type
                           :plain :plain
                           :ref :ref
                           :division))))

(defmethod field-type :plain [_]
  (s/keys :req-un [:heraldry/tincture]))

(defmethod field-type :division [_]
  (s/keys :req-un [:heraldry.field/fields]))

(defmethod field-type :ref [_]
  (s/keys :req-un [:heraldry.field.ref/index]))

(s/def :heraldry/field (s/and (s/multi-spec field-type :heraldry.field/type)
                              (s/keys :opt-un [:heraldry.field/inherit-environment?
                                               :heraldry.field/components
                                               :heraldry/line
                                               :heraldry.field/layout
                                               :heraldry.field/anchor
                                               :heraldry.field/orientation
                                               :heraldry.field/outline?])))

(s/def :heraldry.ordinary/type ordinary.options/ordinary-map)
(s/def :heraldry/ordinary (s/keys :req-un [:heraldry.ordinary/type
                                           :heraldry/field]
                                  :opt-un [:heraldry/line
                                           :heraldry/opposite-line
                                           :heraldry/anchor
                                           :heraldry/orientation
                                           :heraldry/geometry]))

(s/def :heraldry.charge/type keyword?)
(s/def :heraldry.charge/tincture #(every? (fn [[key value]]
                                            (case key
                                              :shadow (or (nil? value)
                                                          (number? value))
                                              :highlight (or (nil? value)
                                                             (number? value))
                                              (or (nil? value)
                                                  (s/valid? :heraldry/tincture value)))) %))
;; TODO: outdated, now outline-mode
;; (s/def :heraldry.charge.hint/outline? boolean?)
;; (s/def :heraldry.charge/hints (s/keys :opt-un [:heraldry.charge.hint/outline?]))
(s/def :heraldry.charge/attitude (s/nilable attributes/attitude-map))
(s/def :heraldry.charge/facing (s/nilable attributes/facing-map))
(s/def :heraldry.charge.variant/id string?)
(s/def :heraldry.charge.variant/version number?)
(s/def :heraldry.charge/variant (s/nilable (s/keys :req-un [:heraldry.charge.variant/id
                                                            :heraldry.charge.variant/version])))
(s/def :heraldry.charge/escutcheon (s/nilable
                                    #(or (= % :none)
                                         (s/valid? :heraldry/escutcheon %))))
(s/def :heraldry/charge (s/keys :req-un [:heraldry.charge/type
                                         :heraldry/field]
                                :opt-un [:heraldry.charge/attitude
                                         :heraldry.charge/facing
                                         :heraldry/position
                                         :heraldry/geometry
                                         :heraldry.charge/escutcheon
                                         :heraldry.charge/tincture
                                         :heraldry.charge/hints
                                         :heraldry.charge/variant]))

;; TODO: add proper spec
(s/def :heraldry.charge-group/type charge-group.options/type-map)
(s/def :heraldry.charge-group/charges (s/coll-of :heraldry/charge :into []))
(s/def :heraldry/charge-group (s/keys :req-un [:heraldry.charge-group/type
                                               :heraldry.charge-group/charges]
                                      :opt-un []))

(s/def :heraldry.semy/type #(= % :heraldry/semy))
(s/def :heraldry.semy/layout #(s/valid? :heraldry.field/layout %))
(s/def :heraldry/semy (s/keys :req-un [:heraldry.semy/type
                                       :heraldry/charge]
                              :opt-un [:heraldry.semy/layout]))

(s/def :heraldry/coat-of-arms (s/keys :req-un [:heraldry/spec-version
                                               :heraldry/field]))

(s/def :heraldry.render-options/escutcheon #(or (= % :none)
                                                (nil? %)
                                                (s/valid? :heraldry/escutcheon %)))
(s/def :heraldry.render-options/mode (s/nilable #{:colours
                                                  :hatching}))
(s/def :heraldry.render-options/shiny? (s/nilable boolean?))
(s/def :heraldry.render-options/outline? (s/nilable boolean?))
(s/def :heraldry.render-options/squiggly? (s/nilable boolean?))
(s/def :heraldry.render-options/theme #(or (nil? %)
                                           (theme/theme-map %)))
(s/def :heraldry.render-options/texture #(or (= % :none)
                                             (nil? %)
                                             (texture/texture-map %)))
(s/def :heraldry.render-options/texture-displacement? (s/nilable boolean?))
(s/def :heraldry/render-options (s/keys :opt-un [:heraldry.render-options/mode
                                                 :heraldry.render-options/shiny?
                                                 :heraldry.render-options/outline?
                                                 :heraldry.render-options/squiggly?
                                                 :heraldry.render-options/theme
                                                 :heraldry.render-options/texture
                                                 :heraldry.render-options/texture-displacement?
                                                 :heraldry.render-options/escutcheon]))
