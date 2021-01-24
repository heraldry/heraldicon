(ns heraldry.spec.coat-of-arms
  (:require [cljs.spec.alpha :as s]
            [heraldry.spec.core :as core]))

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

(s/def :heraldry.geometry/size number?)
(s/def :heraldry/geometry (s/keys :opt-un [:heraldry.geometry/size]))

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
(s/def :heraldry.field/components (s/coll-of #(or (s/valid? :heraldry/ordinary %)
                                                  (s/valid? :heraldry/charge %)) :into []))

(s/def :heraldry/field (s/and (s/keys :req-un [:heraldry/component
                                               (or :heraldry.field/division
                                                   :heraldry.field/content)]
                                      :opt-un [:heraldry.field/inherit-environment?
                                               :heraldry.field/counterchanged?
                                               :heraldry.field/components])
                              #(-> %
                                   (core/get-key :heraldry/component)
                                   (= :field))))

(s/def :heraldry.ordinary/type #{:pale
                                 :fess
                                 :chief
                                 :base
                                 :bend
                                 :bend-sinister
                                 :cross
                                 :saltire
                                 :chevron})
(s/def :heraldry.ordinary/line #(s/valid? :heraldry/line %))
(s/def :heraldry.ordinary/opposite-line #(s/valid? :heraldry/line %))
(s/def :heraldry.ordinary/origin #(s/valid? :heraldry/position %))
(s/def :heraldry.ordinary/field #(s/valid? :heraldry/field %))
(s/def :heraldry.ordinary/geometry #(s/valid? :heraldry/geometry %))
(s/def :heraldry/ordinary (s/and (s/keys :req-un [:heraldry/component
                                                  :heraldry.ordinary/type
                                                  :heraldry.ordinary/field]
                                         :opt-un [:heraldry.ordinary/line
                                                  :heraldry.ordinary/opposite-line
                                                  :heraldry.ordinary/origin
                                                  :heraldry.ordinary/geometry])
                                 #(-> %
                                      (core/get-key :heraldry/component)
                                      (= :ordinary))))

(s/def :heraldry.charge/type keyword?)
(s/def :heraldry.charge/position #(s/valid? :heraldry/position %))
(s/def :heraldry.charge/field #(s/valid? :heraldry/field %))
(s/def :heraldry.charge/geometry #(s/valid? :heraldry/geometry %))
(s/def :heraldry.charge.tincture/armed #(s/valid? :heraldry/tincture %))
(s/def :heraldry.charge.tincture/langued #(s/valid? :heraldry/tincture %))
(s/def :heraldry.charge.tincture/attired #(s/valid? :heraldry/tincture %))
(s/def :heraldry.charge.tincture/unguled #(s/valid? :heraldry/tincture %))
(s/def :heraldry.charge.tincture/beaked #(s/valid? :heraldry/tincture %))
(s/def :heraldry.charge.tincture/eyes-and-teeth #(s/valid? :heraldry/tincture %))
(s/def :heraldry.charge/tincture (s/keys :opt-un [:heraldry.charge.tincture/armed
                                                  :heraldry.charge.tincture/langued
                                                  :heraldry.charge.tincture/attired
                                                  :heraldry.charge.tincture/unguled
                                                  :heraldry.charge.tincture/beaked
                                                  :heraldry.charge.tincture/eyes-and-teeth]))
(s/def :heraldry.charge.hint/outline? boolean?)
(s/def :heraldry.charge/hints (s/keys :opt-un [:heraldry.charge.hint/outline?]))
(s/def :heraldry.charge/attitude #{:none
                                   :couchant
                                   :courant
                                   :dormant
                                   :pascuant
                                   :passant
                                   :rampant
                                   :salient
                                   :sejant
                                   :statant})
(s/def :heraldry.charge/facing #{:none
                                 :to-dexter
                                 :to-sinister
                                 :affronte
                                 :en-arriere
                                 :guardant
                                 :reguardant
                                 :salient
                                 :in-trian-aspect})
(s/def :heraldry.charge.variant/id string?)
(s/def :heraldry.charge.variant/version number?)
(s/def :heraldry.charge/variant (s/keys :req-un [:heraldry.charge.variant/id
                                                 :heraldry.charge.variant/version]))
(s/def :heraldry/charge (s/and (s/keys :req-un [:heraldry/component
                                                :heraldry.charge/type
                                                :heraldry.charge/field]
                                       :opt-un [:heraldry.charge/attitude
                                                :heraldry.charge/facing
                                                :heraldry.charge/position
                                                :heraldry.charge/geometry
                                                :heraldry.charge/tincture
                                                :heraldry.charge/hints
                                                :heraldry.charge/variant])
                               #(-> %
                                    (core/get-key :heraldry/component)
                                    (= :charge))))
