(ns heraldicon.frontend.state
  (:require
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.option.attributes :as attributes]
   [re-frame.core :as rf]))

(rf/reg-sub :used-charge-variants
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path]]
    (->> data
         (tree-seq #(or (map? %)
                        (vector? %)
                        (seq? %)) seq)
         (filter #(and (map? %)
                       (some-> % :type namespace (= "heraldry.charge.type"))
                       (-> % :variant :id)))
         (map :variant)
         set)))

(rf/reg-sub :used-ribbons
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path]]
    (->> data
         (tree-seq #(or (map? %)
                        (vector? %)
                        (seq? %)) seq)
         (filter #(and (map? %)
                       (some-> % :type (isa? :heraldry/motto))
                       (:ribbon-variant %)))
         (map :ribbon-variant)
         set)))

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

(macros/reg-event-db :merge
  (fn [db [_ context value]]
    (if (vector? context)
      (update-in db context merge value)
      (update-in db (:path context) merge value))))

(macros/reg-event-db :update
  (fn [db [_ path update-fn]]
    (update-in db path update-fn)))

(defn remove-element [db path]
  (cond-> db
    (-> path count (= 1)) (dissoc (first path))
    (-> path count (> 1)) (update-in (drop-last path) dissoc (last path))))

(macros/reg-event-db :remove
  (fn [db [_ path]]
    (remove-element db path)))

(defn dispatch-on-event [event effect]
  (rf/dispatch effect)
  (.stopPropagation event))

(defn dispatch-on-event-and-prevent-default [event effect]
  (rf/dispatch effect)
  (doto event
    .preventDefault
    .stopPropagation))
