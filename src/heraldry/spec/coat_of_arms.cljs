(ns heraldry.spec.coat-of-arms
  (:require [cljs.spec.alpha :as s]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.texture :as texture]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.spec.core :as core]))

(s/def :heraldry/spec-version number?)
(s/def :heraldry/escutcheon #(get escutcheon/kinds-map %))
(s/def :heraldry/tincture #(or (= % :none)
                               (get tincture/tincture-map %)))

(s/def :heraldry.line/type (s/nilable line/line-map))
(s/def :heraldry.line/eccentricity (s/nilable number?))
(s/def :heraldry.line/height (s/nilable number?))
(s/def :heraldry.line/width (s/nilable number?))
(s/def :heraldry.line/flipped? (s/nilable boolean?))
(s/def :heraldry/line (s/keys :opt-un [:heraldry.line/type
                                       :heraldry.line/eccentricity
                                       :heraldry.line/height
                                       :heraldry.line/width
                                       :heraldry.line/offset
                                       :heraldry.line/flipped?]))

(s/def :heraldry.position/point (s/nilable position/anchor-point-map))
(s/def :heraldry.position/offset-x (s/nilable number?))
(s/def :heraldry.position/offset-y (s/nilable number?))
(s/def :heraldry/position (s/keys :opt-un [:heraldry.position/point
                                           :heraldry.position/offset-x
                                           :heraldry.position/offset-y]))

(s/def :heraldry/origin #(s/valid? :heraldry/position %))
(s/def :heraldry/anchor #(s/valid? :heraldry/position %))

(s/def :heraldry.geometry/size number?)
(s/def :heraldry/geometry (s/keys :opt-un [:heraldry.geometry/size]))

(s/def :heraldry.field.layout/num-base-fields (s/nilable number?))
(s/def :heraldry.field.layout/num-fields-x (s/nilable number?))
(s/def :heraldry.field.layout/num-fields-y (s/nilable number?))
(s/def :heraldry.field.layout/offset-x (s/nilable number?))
(s/def :heraldry.field.layout/offset-y (s/nilable number?))
(s/def :heraldry.field.layout/stretch-x (s/nilable number?))
(s/def :heraldry.field.layout/stretch-y (s/nilable number?))
(s/def :heraldry.field.layout/rotation (s/nilable number?))
(s/def :heraldry.field/layout (s/keys :opt-un [:heraldry.field.layout/num-base-fields
                                               :heraldry.field.layout/num-fields-x
                                               :heraldry.field.layout/num-fields-y
                                               :heraldry.field.layout/offset-x
                                               :heraldry.field.layout/offset-y
                                               :heraldry.field.layout/stretch-x
                                               :heraldry.field.layout/stretch-y
                                               :heraldry.field.layout/rotation]))
(s/def :heraldry.field/fields (s/coll-of :heraldry/field :into []))
(s/def :heraldry.field.hint/outline? boolean?)
(s/def :heraldry.field/hints (s/keys :opt-un [:heraldry.hint/outline?]))
(s/def :heraldry.field/inherit-environment? boolean?)
(s/def :heraldry.field/counterchanged? boolean?)
(s/def :heraldry.field/components (s/coll-of #(or (s/valid? :heraldry/ordinary %)
                                                  (s/valid? :heraldry/charge %)
                                                  (s/valid? :heraldry/semy %)) :into []))

(s/def :heraldry.field.ref/index #(and (number? %)
                                       (>=  % 0)))

(defmulti field-type (fn [field]
                       (let [type ((some-fn :heraldry.field/type :type) field)
                             type ;; strip the namespace, but only if it is the right one
                             (if (some-> type namespace (= "heraldry.field.type"))
                               (-> type name keyword)
                               type)]
                         (case type
                           :plain :plain
                           :ref   :ref
                           :division))))

(defmethod field-type :plain [_]
  (s/keys :req-un [:heraldry/tincture]))

(defmethod field-type :division [_]
  (s/keys :req-un [:heraldry.field/fields]))

(defmethod field-type :ref [_]
  (s/keys :req-un [:heraldry.field.ref/index]))

(s/def :heraldry/field (s/and (s/multi-spec field-type :heraldry.field/type)
                              (s/keys :opt-un [:heraldry.field/inherit-environment?
                                               :heraldry.field/counterchanged?
                                               :heraldry.field/components
                                               :heraldry/line
                                               :heraldry.field/layout
                                               :heraldry.field/origin
                                               :heraldry.field/anchor
                                               :heraldry.field/hints])))

(s/def :heraldry.ordinary/type ordinary/ordinary-map)
(s/def :heraldry/ordinary (s/keys :req-un [:heraldry.ordinary/type
                                           :heraldry/field]
                                  :opt-un [:heraldry/line
                                           :heraldry/opposite-line
                                           :heraldry/origin
                                           :heraldry/anchor
                                           :heraldry/geometry]))

(s/def :heraldry.charge/type keyword?)
(s/def :heraldry.charge/tincture #(every? (fn [[key value]]
                                            (case key
                                              :shadow    (number? value)
                                              :highlight (number? value)
                                              (s/valid? :heraldry/tincture value))) %))
(s/def :heraldry.charge.hint/outline? boolean?)
(s/def :heraldry.charge/hints (s/keys :opt-un [:heraldry.charge.hint/outline?]))
(s/def :heraldry.charge/attitude  (s/nilable attributes/attitude-map))
(s/def :heraldry.charge/facing  (s/nilable attributes/facing-map))
(s/def :heraldry.charge.variant/id string?)
(s/def :heraldry.charge.variant/version number?)
(s/def :heraldry.charge/variant (s/nilable (s/keys :req-un [:heraldry.charge.variant/id
                                                            :heraldry.charge.variant/version])))
(s/def :heraldry.charge/escutcheon #(or (= % :root)
                                        (s/valid? :heraldry/escutcheon %)))
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

(s/def :heraldry.semy/type #(= % :heraldry.component/semy))
(s/def :heraldry.semy/layout #(s/valid? :heraldry.field/layout %))
(s/def :heraldry/semy (s/keys :req-un [:heraldry.semy/type
                                       :heraldry/charge]
                              :opt-un [:heraldry.semy/layout]))

(s/def :heraldry/coat-of-arms (s/keys :req-un [:heraldry/spec-version
                                               :heraldry/escutcheon
                                               :heraldry/field]))

(s/def :heraldry.render-options/escutcheon-override #(or (= % :none)
                                                         (s/valid? :heraldry/escutcheon %)))
(s/def :heraldry.render-options/mode #{:colours
                                       :hatching})
(s/def :heraldry.render-options/shiny? boolean?)
(s/def :heraldry.render-options/outline? boolean?)
(s/def :heraldry.render-options/squiggly? boolean?)
(s/def :heraldry.render-options/theme tincture/theme-map)
(s/def :heraldry.render-options/texture #(or (= % :none)
                                             (texture/texture-map %)))
(s/def :heraldry.render-options/texture-displacement? boolean?)
(s/def :heraldry/render-options (s/keys :opt-un [:heraldry.render-options/mode
                                                 :heraldry.render-options/shiny?
                                                 :heraldry.render-options/outline?
                                                 :heraldry.render-options/squiggly?
                                                 :heraldry.render-options/theme
                                                 :heraldry.render-options/texture
                                                 :heraldry.render-options/texture-displacement?
                                                 :heraldry.render-options/escutcheon-override]))

