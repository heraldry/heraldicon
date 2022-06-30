(ns heraldicon.frontend.state
  (:require
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.option.attributes :as attributes]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [re-frame.core :as rf])
  (:require-macros [reagent.ratom :refer [reaction]]))

(macros/reg-event-db :clear-db
  (fn [_ _]
    {}))

(def ^:private db-defaults
  {:example-coa {:render-options (assoc default/render-options
                                        :escutcheon :rectangle)
                 :coat-of-arms {:field {:type :heraldry.field.type/plain
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
                                                                        :secondary :gules
                                                                        :tertiary :vert
                                                                        :armed :or
                                                                        :langued :gules
                                                                        :attired :argent
                                                                        :unguled :vert
                                                                        :beaked :or
                                                                        :winged :purpure
                                                                        :pommeled :gules
                                                                        :shadow 1.0
                                                                        :highlight 1.0})}]}}}
   :ui {:charge-tree {:show-public? true
                      :show-own? true}
        :component-tree {}}})

(macros/reg-event-db :initialize-db
  (fn [db [_ crawler?]]
    (merge-with merge (assoc-in db-defaults [:ui :crawler?] crawler?) db)))

(macros/reg-event-db :set
  (fn [db [_ context value]]
    (if (vector? context)
      (assoc-in db context value)
      (assoc-in db (:path context) value))))

(defn dispatch-on-event [event effect]
  (rf/dispatch effect)
  (.stopPropagation event))

(defn dispatch-on-event-and-prevent-default [event effect]
  (rf/dispatch effect)
  (doto event
    .preventDefault
    .stopPropagation))

(rf/reg-sub :get
  (fn [db [_ path]]
    (if (map? path)
      (get-in db (:path path))
      (get-in db path))))

(rf/reg-sub :get-list-size
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    (count value)))

(rf/reg-sub :nil?
  (fn [db [_ path]]
    (nil? (get-in db path))))

(rf/reg-sub ::component-type
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :type)]))

  (fn [raw-type _]
    (component/effective-type raw-type)))

(rf/reg-sub ::options-subscriptions-data
  (fn [[_ path] _]
    [(rf/subscribe [::component-type path])
     (rf/subscribe [:get (conj path :type)])])

  (fn [[component-type entity-type] [_ path]]
    (when (isa? component-type :heraldry.options/root)
      (let [context {:path path
                     :dispatch-value component-type
                     :entity-type entity-type}]
        (assoc context :required-subscriptions (interface/options-subscriptions context))))))

(rf/reg-sub-raw ::options
  (fn [_app-db [_ path]]
    (reaction
     (when (seq path)
       (if-let [context @(rf/subscribe [::options-subscriptions-data path])]
         (-> context
             (assoc :subscriptions {:base-path path
                                    :data (into {}
                                                (map (fn [relative-path]
                                                       [relative-path
                                                        @(rf/subscribe [:get (vec (concat path relative-path))])]))
                                                (:required-subscriptions context))})
             interface/options)
         (get @(rf/subscribe [::options (pop path)]) (last path)))))))

(rf/reg-sub ::sanitized-data
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [::options path])])

  (fn [[data options] [_ _path]]
    (options/sanitize-value-or-data data options)))
