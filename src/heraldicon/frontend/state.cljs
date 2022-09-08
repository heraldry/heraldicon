(ns heraldicon.frontend.state
  (:require
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.option.attributes :as attributes]))

(macros/reg-event-db ::clear-db
  (fn [_ _]
    {}))

(def ^:private db-defaults
  {:example-coa {:render-options (assoc default/render-options
                                        :escutcheon-shadow? true
                                        :escutcheon :rectangle)
                 :coat-of-arms {:type :heraldry/coat-of-arms
                                :field {:type :heraldry.field.type/plain
                                        :tincture :argent
                                        :components [{:type :heraldry.charge.type/preview
                                                      :preview? true
                                                      :ignore-layer-separator? true
                                                      :field {:type :heraldry.field.type/plain
                                                              :tincture :azure}
                                                      :geometry {:size 95}
                                                      :tincture (merge (into {}
                                                                             (map (fn [[k _]]
                                                                                    [k :or]))
                                                                             attributes/tincture-modifier-map)
                                                                       {:orbed :argent
                                                                        :eyed :argent
                                                                        :toothed :argent
                                                                        :secondary :or
                                                                        :tertiary :gules
                                                                        :armed :or
                                                                        :langued :gules
                                                                        :attired :argent
                                                                        :unguled :vert
                                                                        :beaked :or
                                                                        :winged :purpure
                                                                        :pommeled :gules
                                                                        :shadow 1.0
                                                                        :highlight 1.0})}]}}}
   :ui {:empty-coat-of-arms default/coat-of-arms}})

(macros/reg-event-db ::initialize
  (fn [db [_ crawler?]]
    (merge-with merge (assoc-in db-defaults [:ui :crawler?] crawler?) db)))
