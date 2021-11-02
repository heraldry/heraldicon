(ns heraldry.frontend.state
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.coat-of-arms.attributes :as attributes]
   [heraldry.coat-of-arms.default :as default]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.form.collection-element :as collection-element]
   [heraldry.interface :as interface]
   [heraldry.shield-separator :as shield-separator]
   [heraldry.util :as util]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def title-path [:ui :title])

;; subs

(rf/reg-sub :get
  (fn [db [_ path]]
    (get-in db path)))

(rf/reg-sub :get-form-error
  (fn [db [_ path]]
    (get-in db (concat [:form-errors] path [:message]))))

(rf/reg-sub :get-form-message
  (fn [db [_ path]]
    (get-in db (concat [:form-message] path [:message]))))

(rf/reg-sub :get-title
  (fn [_ _]
    [(rf/subscribe [:get title-path])
     (rf/subscribe [:heraldry.frontend.language/selected-language])])

  (fn [[value selected-language] [_ _path]]
    (util/tr-raw value selected-language)))

(rf/reg-sub :get-list-size
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    (count value)))

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
                       (-> % :variant :version)
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
                       (some-> % :type namespace (= "heraldry.motto.type"))
                       (-> % :ribbon-variant)))
         (map :ribbon-variant)
         set)))

;; events


(macros/reg-event-db :initialize-db
  (fn [db [_]]
    (merge {:example-coa {:render-options (assoc default/render-options
                                                 :escutcheon :rectangle)
                          :coat-of-arms {:field {:type :heraldry.field.type/plain
                                                 :tincture :argent
                                                 :components [{:type :heraldry.charge.type/preview
                                                               :preview? true
                                                               :ignore-layer-separator? true
                                                               :field {:type :heraldry.field.type/plain
                                                                       :tincture :azure}
                                                               :geometry {:size 95}
                                                               :tincture (merge (->> attributes/tincture-modifier-map
                                                                                     (map (fn [[k _]]
                                                                                            [k :or]))
                                                                                     (into {}))
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
            :coat-of-arms {:escutcheon :rectangle}
            :ui {:charge-tree {:show-public? true
                               :show-own? true}
                 :component-tree {}}}
           db)))

(macros/reg-event-db :set
  (fn [db [_ path value]]
    (assoc-in db path value)))

(macros/reg-event-db :set-title
  (fn [db [_ value]]
    (assoc-in db title-path value)))

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

(macros/reg-event-db :set-form-error
  (fn [db [_ db-path error]]
    (assoc-in db (concat [:form-errors] db-path [:message]) error)))

(macros/reg-event-db :set-form-message
  (fn [db [_ db-path message]]
    (assoc-in db (concat [:form-message] db-path [:message]) message)))

(macros/reg-event-db :clear-form-errors
  (fn [db [_ db-path]]
    (remove-element db (into [:form-errors] db-path))))

(macros/reg-event-db :clear-form-message
  (fn [db [_ db-path]]
    (remove-element db (into [:form-message] db-path))))

(macros/reg-event-db :clear-form
  (fn [db [_ db-path]]
    (-> db
        (remove-element (into [:form-errors] db-path))
        (remove-element (into [:form-message] db-path))
        (remove-element db-path))))

(defn dispatch-on-event [event effect]
  (rf/dispatch effect)
  (.stopPropagation event))

(defn set-async-fetch-data [db-path query-id data]
  (rf/dispatch-sync [:set [:async-fetch-data db-path :current] query-id])
  (rf/dispatch-sync [:set [:async-fetch-data db-path :queries query-id] {:state :done
                                                                         :data data}])
  (rf/dispatch-sync [:set db-path data]))

;; TODO: this should be redone and also live elsewhere probably
(defn async-fetch-data [db-path query-id async-function]
  (let [current-query @(rf/subscribe [:get [:async-fetch-data db-path :current]])
        query @(rf/subscribe [:get [:async-fetch-data db-path :queries query-id]])
        current-data @(rf/subscribe [:get db-path])]
    (cond
      (not= current-query query-id) (do
                                      (rf/dispatch-sync [:set [:async-fetch-data db-path :current] query-id])
                                      (rf/dispatch-sync [:set db-path nil])
                                      [:loading nil])
      current-data [:done current-data]
      (-> query :state (= :done)) (do
                                    (rf/dispatch-sync [:set db-path (:data query)])
                                    [:done (:data query)])
      (-> query :state) [:loading nil]
      :else (do
              (go
                (try
                  (when-not (:state @(rf/subscribe [:get [:async-fetch-data db-path :queries query-id]]))
                    (rf/dispatch-sync [:set db-path nil])
                    (rf/dispatch-sync [:set [:async-fetch-data db-path :current] query-id])
                    (rf/dispatch-sync [:set [:async-fetch-data db-path :queries query-id]
                                       {:state :loading}])
                    (let [data (<? (async-function))]
                      (rf/dispatch-sync [:set [:async-fetch-data db-path :queries query-id]
                                         {:state :done
                                          :data data}]))
                    (rf/dispatch-sync [:set [:async-fetch-data db-path :current] query-id]))
                  (catch :default e
                    (log/error "async fetch data error:" db-path query-id e))))
              [:loading nil]))))

(defn invalidate-cache-without-current [db-path query-id]
  (rf/dispatch-sync [:set [:async-fetch-data db-path :queries query-id] nil]))

(defn invalidate-cache [db-path query-id]
  (invalidate-cache-without-current db-path query-id)
  (let [current-query @(rf/subscribe [:get [:async-fetch-data db-path :current]])]
    (when (= current-query query-id)
      (rf/dispatch-sync [:set [:async-fetch-data db-path :current] nil])
      (rf/dispatch-sync [:set db-path nil]))))

(defn invalidate-cache-all-but-new []
  (rf/dispatch-sync [:update [:async-fetch-data]
                     (fn [async-data]
                       (->> async-data
                            (map (fn [[k v]]
                                   [k (-> v
                                          (update :queries select-keys [:new])
                                          (update :current #(when (= % :new) :new)))]))
                            (into {})))]))

(def node-flag-db-path [:ui :component-tree :nodes])
(def ui-submenu-open?-path [:ui :submenu-open?])
(def ui-component-node-selected-path [:ui :component-tree :selected-node])
(def ui-component-node-selected-default-path [:ui :component-tree :selected-node-default])

(defn component-node-open-by-default? [path]
  (or (->> path (take-last 1) #{[:coat-of-arms]
                                [:helms]
                                [:ornaments]})
      (->> path (take-last 2) #{[:coat-of-arms :field]
                                [:collection-form :collection]})
      (->> path (take-last 5) #{[:example-coa :coat-of-arms :field :components 0]})))

(defn component-node-open? [flag path]
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

(defn ui-component-node-open [db path]
  (let [path (vec path)]
    (->> (range (count path))
         (map (fn [idx]
                [(subvec path 0 (inc idx)) true]))
         (into {})
         (update-in db node-flag-db-path merge))))

(defn ui-component-node-close [db path]
  (update-in
   db node-flag-db-path
   (fn [flags]
     (->> (assoc flags path false)
          (map (fn [[other-path v]]
                 (if (= (take (count path) other-path)
                        path)
                   [other-path false]
                   [other-path v])))
          (into {})))))

(macros/reg-event-db :ui-component-node-toggle
  (fn [db [_ path]]
    (if (component-node-open?
         (get-in db (conj node-flag-db-path path))
         path)
      (ui-component-node-close db path)
      (ui-component-node-open db path))))

(defn ui-component-node-select [db path & {:keys [open?]}]
  (let [raw-type (get-in db (conj path :type))
        component-type (interface/effective-component-type path raw-type)]
    (-> db
        (assoc-in ui-component-node-selected-path path)
        (ui-component-node-open (cond-> path
                                  (not open?) drop-last))
        (cond->
          (= component-type :heraldry.component/collection-element)
          (assoc-in collection-element/ui-highlighted-element-path path)))))

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

(defn adjust-component-path-after-order-change [path elements-path index new-index]
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
      (update-in collection-element/ui-highlighted-element-path
                 adjust-component-path-after-order-change elements-path index new-index)
      (update-in ui-submenu-open?-path
                 (fn [flags]
                   (->> flags
                        (map (fn [[k v]]
                               [(adjust-component-path-after-order-change
                                 k elements-path index new-index) v]))
                        (filter first)
                        (into {}))))
      (update-in node-flag-db-path (fn [flags]
                                     (->> flags
                                          (keep (fn [[path flag]]
                                                  (let [new-path (adjust-component-path-after-order-change
                                                                  path elements-path index new-index)]
                                                    (when new-path
                                                      [new-path flag]))))
                                          (into {}))))))
