(ns heraldry.frontend.state
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.default :as default]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

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

(rf/reg-sub :get-value
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [value [_ _path]]
    value))

;; events

(rf/reg-event-db :initialize-db
  (fn [db [_]]
    (merge {:example-coa {:render-options default/render-options
                          :coat-of-arms {:escutcheon :rectangle
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :argent
                                                 :components [{:type :heraldry.charge.type/preview
                                                               :preview? true
                                                               :field {:type :heraldry.field.type/plain
                                                                       :tincture :azure}
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

(rf/reg-event-db :set
  (fn [db [_ path value]]
    (assoc-in db path value)))

(rf/reg-event-db :update
  (fn [db [_ path update-fn]]
    (update-in db path update-fn)))

(rf/reg-event-db :remove
  (fn [db [_ path]]
    (cond-> db
      (-> path count (= 1)) (dissoc (first path))
      (-> path count (> 1)) (update-in (drop-last path) dissoc (last path)))))

(rf/reg-event-db :set-form-error
  (fn [db [_ db-path error]]
    (assoc-in db (concat [:form-errors] db-path [:message]) error)))

(rf/reg-event-db :set-form-message
  (fn [db [_ db-path message]]
    (assoc-in db (concat [:form-message] db-path [:message]) message)))

(rf/reg-event-fx :clear-form-errors
  (fn [_ [_ db-path]]
    {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]]}))

(rf/reg-event-fx :clear-form-message
  (fn [_ [_ db-path]]
    {:fx [[:dispatch [:remove (into [:form-message] db-path)]]]}))

(rf/reg-event-fx :clear-form
  (fn [_ [_ db-path]]
    {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]
          [:dispatch [:remove (into [:form-message] db-path)]]
          [:dispatch [:remove db-path]]]}))

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
              (rf/dispatch-sync [:set db-path nil])
              (rf/dispatch-sync [:set [:async-fetch-data db-path :current] query-id])
              (rf/dispatch-sync [:set [:async-fetch-data db-path :queries query-id]
                                 {:state :loading}])
              (go
                (try
                  (let [data (<? (async-function))]
                    (rf/dispatch-sync [:set [:async-fetch-data db-path :queries query-id]
                                       {:state :done
                                        :data data}]))
                  (rf/dispatch-sync [:set [:async-fetch-data db-path :current] query-id])
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
(def ui-component-node-selected-path [:ui :component-tree :selected-node])

(defn ui-component-node-open [db path]
  (let [path (vec path)]
    (->> (range (count path))
         (map (fn [idx]
                [(subvec path 0 (inc idx)) true]))
         (into {})
         (update-in db node-flag-db-path merge))))

(rf/reg-event-db :ui-component-node-open
  (fn [db [_ path]]
    (ui-component-node-open db path)))

(defn ui-component-node-close [db path]
  (update-in
   db node-flag-db-path
   (fn [flags]
     (->> flags
          (filter (fn [[other-path v]]
                    (when (not= (take (count path) other-path)
                                path)
                      [other-path v])))
          (into {})))))

(rf/reg-event-db :ui-component-node-close
  (fn [db [_ path]]
    (ui-component-node-close db path)))

(rf/reg-event-db :ui-component-node-toggle
  (fn [db [_ path]]
    (if (get-in db (conj node-flag-db-path path))
      (ui-component-node-close db path)
      (ui-component-node-open db path))))

(defn ui-component-node-select [db path & {:keys [open?]}]
  (-> db
      (assoc-in ui-component-node-selected-path path)
      (ui-component-node-open (cond-> path
                                (not open?) drop-last))))

(rf/reg-event-db :ui-component-node-select
  (fn [db [_ path {:keys [open?]}]]
    (ui-component-node-select db path :open? open?)))

(rf/reg-event-db :ui-component-node-select-default
  (fn [db [_ path]]
    (let [old-path (get-in db ui-component-node-selected-path)
          new-path (if (or (not old-path)
                           (not= (subvec old-path 0 (count path))
                                 path))
                     path
                     old-path)]
      (if (not= new-path old-path)
        (ui-component-node-select db new-path)
        db))))
