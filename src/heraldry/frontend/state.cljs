(ns heraldry.frontend.state
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.default :as default]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]
            [taoensso.tufte :as tufte]))

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

(defonce stats-accumulator (tufte/add-accumulating-handler! {:ns-pattern "*"}))

(defn display-profiler-stats []
  (when-let [m (not-empty @stats-accumulator)]
    (js/console.log (tufte/format-grouped-pstats
                     m
                     {:format-pstats-opts {:columns [:n-calls :p95 :mean :clock :total]}}))))

(rf/reg-event-db :initialize-db
  (fn [db [_]]
    (when-let [profile-timer (:profile-timer db)]
      (js/clearInterval profile-timer))
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
           db
           {:profile-timer (js/setInterval display-profiler-stats 5000)})))

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

(rf/reg-event-fx :set-field-layout-num-fields-x
  (fn [{:keys [db]} [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      {:dispatch [:set-field-type
                  field-path
                  (:type field)
                  value
                  (-> field :layout :num-fields-y)
                  (-> field :layout :num-base-fields)]})))

(rf/reg-event-fx :set-field-layout-num-fields-y
  (fn [{:keys [db]} [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      {:dispatch [:set-field-type
                  field-path
                  (:type field)
                  (-> field :layout :num-fields-x)
                  value
                  (-> field :layout :num-base-fields)]})))

(rf/reg-event-fx :set-field-layout-num-base-fields
  (fn [{:keys [db]} [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      {:dispatch [:set-field-type
                  field-path
                  (:type field)
                  (-> field :layout :num-fields-x)
                  (-> field :layout :num-fields-y)
                  value]})))
