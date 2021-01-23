(ns heraldry.spec.core
  (:require [cljs.spec.alpha :as s]))

(defn get-key [coll key]
  (or (get coll key)
      (get coll (-> key name keyword))))

(s/def :heraldry/tincture #{:none
                            :argent
                            :or
                            :vert
                            :azure
                            :gules
                            :sable
                            :purpure
                            :murrey
                            :sanguine
                            :tenne
                            :ermine
                            :ermines
                            :erminois
                            :pean})

(s/def :heraldry/component #{:field
                             :ordinary
                             :charge})

(s/def :heraldry.line/type #{:straight
                             :invected
                             :engrailed
                             :embattled
                             :indented
                             :dancetty
                             :wavy
                             :dovetailed
                             :raguly
                             :urdy})
(s/def :heraldry.line/eccentricity number?)
(s/def :heraldry.line/width number?)
(s/def :heraldry.line/flipped? boolean?)
(s/def :heraldry/line (s/keys :opt-un [:heraldry.line/type
                                       :heraldry.line/eccentricity
                                       :heraldry.line/width
                                       :heraldry.line/offset
                                       :heraldry.line/flipped?]))

(s/def :heraldry.position/point #{:fess
                                  :chief
                                  :base
                                  :dexter
                                  :sinister
                                  :honour
                                  :nombril})
(s/def :heraldry.position/offset-x number?)
(s/def :heraldry.position/offset-y number?)
(s/def :heraldry/position (s/keys :opt-un [:heraldry.position/point
                                           :heraldry.position/offset-x
                                           :heraldry.position/offset-y]))

(s/def :heraldry.field.division/type #{:per-pale
                                       :per-fess
                                       :per-bend
                                       :per-bend-sinister
                                       :per-chevron
                                       :per-saltire
                                       :quarterly
                                       :gyronny
                                       :tierced-per-pale
                                       :tierced-per-fess
                                       :tierced-per-pairle
                                       :tierced-per-pairle-reversed})
(s/def :heraldry.field.division/line #(s/valid? :heraldry/line %))
(s/def :heraldry.field.division/origin #(s/valid? :heraldry/position %))
(s/def :heraldry.field.division/fields (s/coll-of :heraldry/field :into []))
(s/def :heraldry.field.divison.hint/outline? boolean?)
(s/def :heraldry.field.division/hints (s/keys :opt-un [:heraldry.division.hint/outline?]))
(s/def :heraldry.field/division (s/keys :req-un [:heraldry.field.division/type
                                                 :heraldry.field.division/fields]
                                        :opt-un [:heraldry.field.division/line
                                                 :heraldry.field.division/origin
                                                 :heraldry.field.division/hints]))
(s/def :heraldry.field/content (s/keys :req-un [:heraldry/tincture]))
(s/def :heraldry.field/inherit-environment? boolean?)
(s/def :heraldry.field/counterchanged? boolean?)

(s/def :heraldry/field (s/and (s/keys :req-un [:heraldry/component
                                               (or :heraldry.field/division
                                                   :heraldry.field/content)]
                                      :opt-un [:heraldry.field/inherit-environment?
                                               :heraldry.field/counterchanged?])
                              #(-> %
                                   (get-key :heraldry/component)
                                   (= :field))))
