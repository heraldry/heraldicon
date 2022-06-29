(ns heraldicon.frontend.state
  (:require
   [heraldicon.frontend.component.entity.collection.element :as collection.element]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.component :as component]
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

(def ^:private node-flag-db-path
  [:ui :component-tree :nodes])

(def ui-submenu-open?-path
  [:ui :submenu-open?])

(def ^:private ui-component-node-selected-path
  [:ui :component-tree :selected-node])

(def ^:private ui-component-node-selected-default-path
  [:ui :component-tree :selected-node-default])

(defn- component-node-open-by-default? [path]
  (or (#{[:coat-of-arms]
         [:helms]
         [:ornaments]
         [:elements]}
       (take-last 1 path))
      (#{[:coat-of-arms :field]}
       (take-last 2 path))
      (#{;; TODO: path shouldn't be hard-coded
         [:heraldicon.entity/collection :data :data]}
       (take-last 3 path))
      (#{[:example-coa :coat-of-arms :field :components 0]}
       (take-last 5 path))))

(defn- component-node-open? [flag path]
  (if (nil? flag)
    (component-node-open-by-default? path)
    flag))

(rf/reg-sub :ui-component-node-open?
  (fn [[_ path] _]
    (rf/subscribe [:get (conj node-flag-db-path path)]))

  (fn [flag [_ path]]
    (component-node-open? flag path)))

(rf/reg-sub :ui-component-node-selected-path
  (fn [_ _]
    [(rf/subscribe [:get ui-component-node-selected-path])
     (rf/subscribe [:get @(rf/subscribe [:get ui-component-node-selected-path])])
     (rf/subscribe [:get ui-component-node-selected-default-path])])

  (fn [[selected-node-path data default] [_ _path]]
    (if (and selected-node-path
             data)
      selected-node-path
      default)))

(defn- ui-component-node-open [db path]
  (let [path (vec path)]
    (update-in db node-flag-db-path
               merge (into {}
                           (map (fn [idx]
                                  [(subvec path 0 (inc idx)) true]))
                           (range (count path))))))

(defn- ui-component-node-close [db path]
  (update-in
   db node-flag-db-path
   (fn [flags]
     (into {}
           (map (fn [[other-path v]]
                  (if (= (take (count path) other-path)
                         path)
                    [other-path false]
                    [other-path v])))
           (assoc flags path false)))))

(macros/reg-event-db :ui-component-node-toggle
  (fn [db [_ path]]
    (if (component-node-open?
         (get-in db (conj node-flag-db-path path))
         path)
      (ui-component-node-close db path)
      (ui-component-node-open db path))))

(defn ui-component-node-select [db path & {:keys [open?]}]
  (let [raw-type (get-in db (conj path :type))
        component-type (component/effective-type raw-type)]
    (-> db
        (assoc-in ui-component-node-selected-path path)
        (ui-component-node-open (cond-> path
                                  (not open?) drop-last))
        (cond->
          (= component-type :heraldicon.entity.collection/element)
          (assoc-in collection.element/highlighted-element-path path)))))

(macros/reg-event-db :ui-component-node-select
  (fn [db [_ path {:keys [open?]}]]
    (ui-component-node-select db path :open? open?)))

(macros/reg-event-db :ui-component-node-select-default
  (fn [db [_ path valid-prefixes]]
    (let [current-selected-node (get-in db ui-component-node-selected-path)
          valid-path? (some (fn [path]
                              (= (take (count path) current-selected-node)
                                 path))
                            valid-prefixes)]
      (-> db
          (assoc-in ui-component-node-selected-default-path path)
          (cond->
            (not valid-path?) (assoc-in ui-component-node-selected-path nil))))))

(defn- adjust-component-path-after-order-change [path elements-path index new-index]
  (let [elements-path-size (count elements-path)
        path-base (when (-> path count (>= elements-path-size))
                    (subvec path 0 elements-path-size))
        path-rest (when (-> path count (>= elements-path-size))
                    (subvec path (count elements-path)))
        current-index (first path-rest)
        path-rest (when (-> path-rest count (> 1))
                    (subvec path-rest 1))]
    (if (or (not= path-base elements-path)
            (= index new-index))
      path
      (if (nil? new-index)
        (cond
          (= current-index index) nil
          (> current-index index) (vec (concat path-base
                                               [(dec current-index)]
                                               path-rest))
          :else path)
        (cond
          (= current-index index) (vec (concat path-base
                                               [new-index]
                                               path-rest))
          (<= index current-index new-index) (vec (concat path-base
                                                          [(dec current-index)]
                                                          path-rest))
          (<= new-index current-index index) (vec (concat path-base
                                                          [(inc current-index)]
                                                          path-rest))
          :else path)))))

(defn change-selected-component-if-removed [db fallback-path]
  (if (get-in db (get-in db ui-component-node-selected-path))
    db
    (assoc-in db ui-component-node-selected-path fallback-path)))

(defn element-order-changed [db elements-path index new-index]
  (-> db
      (update-in ui-component-node-selected-path
                 adjust-component-path-after-order-change elements-path index new-index)
      (update-in collection.element/highlighted-element-path
                 adjust-component-path-after-order-change elements-path index new-index)
      (update-in ui-submenu-open?-path
                 (fn [flags]
                   (into {}
                         (comp (filter first)
                               (map (fn [[k v]]
                                      [(adjust-component-path-after-order-change
                                        k elements-path index new-index) v])))
                         flags)))
      (update-in node-flag-db-path (fn [flags]
                                     (into {}
                                           (keep (fn [[path flag]]
                                                   (let [new-path (adjust-component-path-after-order-change
                                                                   path elements-path index new-index)]
                                                     (when new-path
                                                       [new-path flag]))))
                                           flags)))))
