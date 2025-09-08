(ns heraldicon.frontend.state
  (:require
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.option.attributes :as attributes]))

(macros/reg-event-db ::clear-db
  (fn [_ _]
    {}))

(def ^:private db-defaults
  {:ui {:empty-coat-of-arms default/coat-of-arms
        :charge-original {:render-options (assoc default/render-options
                                                 :escutcheon-shadow? true
                                                 :escutcheon :rectangle)
                          :coat-of-arms {:type :heraldry/coat-of-arms
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :argent
                                                 :components [{:type :heraldry.charge.type/preview
                                                               :variant {:id :form-data-original
                                                                         :version :form-data-original}
                                                               :preview? true
                                                               :field {:type :heraldry.field.type/plain
                                                                       :tincture :azure}
                                                               :geometry {:size 95}}]}}}
        :charge-preview {:render-options (assoc default/render-options
                                                :escutcheon-shadow? true
                                                :escutcheon :rectangle)
                         :coat-of-arms {:type :heraldry/coat-of-arms
                                        :field {:type :heraldry.field.type/plain
                                                :tincture :argent
                                                :components [{:type :heraldry.charge.type/preview
                                                              :variant {:id :form-data
                                                                        :version :form-data}
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
                                                                                :highlight 1.0})}]}}}}})

(macros/reg-event-db ::initialize
  (fn [db [_ crawler? crawler-next-list-page]]
    (->> db
         (merge-with merge (-> db-defaults
                               (assoc-in [:ui :crawler?] crawler?)
                               (assoc-in [:ui :crawler-next-list-page] crawler-next-list-page))))))
