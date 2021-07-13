(ns heraldry.frontend.state
  (:require [cljs.core.async :refer [go]]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<?]]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
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
  (fn-traced [db [_]]
    (when-let [profile-timer (:profile-timer db)]
      (js/clearInterval profile-timer))
    (merge {:example-coa {:render-options default/render-options
                          :coat-of-arms {:escutcheon :rectangle
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :argent
                                                 :components [{:type :heraldry.charge.type/preview
                                                               :field {:type :heraldry.field.type/plain
                                                                       :tincture :azure}
                                                               :tincture (merge (->> attributes/tincture-modifier-map
                                                                                     (map (fn [[k _]]
                                                                                            [k :or]))
                                                                                     (into {}))
                                                                                {:eyes-and-teeth :argent
                                                                                 :orbed :argent
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
            :ui {:component-open? {[:arms-form :render-options] true
                                   [:arms-form :coat-of-arms] true
                                   [:arms-form :coat-of-arms :field] true
                                   [:arms-form :attribution] true
                                   [:charge-form :attribution] true
                                   [:example-coa :render-options] true
                                   [:example-coa :coat-of-arms] true
                                   [:example-coa :coat-of-arms :field] true}
                 :charge-tree {:show-public? true
                               :show-own? true}
                 :component-tree {:selected-node [:arms-form]}}}
           db
           {:profile-timer (js/setInterval display-profiler-stats 5000)})))

(rf/reg-event-db :set
  (fn-traced [db [_ path value]]
    (assoc-in db path value)))

(rf/reg-event-db :update
  (fn-traced [db [_ path update-fn]]
    (update-in db path update-fn)))

(rf/reg-event-db :remove
  (fn-traced [db [_ path]]
    (cond-> db
      (-> path count (= 1)) (dissoc (first path))
      (-> path count (> 1)) (update-in (drop-last path) dissoc (last path)))))

(rf/reg-event-db :toggle
  (fn-traced [db [_ path]]
    (update-in db path not)))

(rf/reg-event-db :set-form-error
  (fn-traced [db [_ db-path error]]
    (assoc-in db (concat [:form-errors] db-path [:message]) error)))

(rf/reg-event-db :set-form-message
  (fn-traced [db [_ db-path message]]
    (assoc-in db (concat [:form-message] db-path [:message]) message)))

(defn normalize-tag [tag]
  (let [normalized-tag (-> tag
                           (cond->
                            (keyword? tag) name)
                           (or "")
                           s/trim
                           s/lower-case)]
    (when (-> normalized-tag
              count
              pos?)
      (keyword normalized-tag))))

(rf/reg-event-db :add-tags
  (fn-traced [db [_ db-path tags]]
    (update-in db db-path (fn [current-tags]
                            (-> current-tags
                                keys
                                set
                                (concat tags)
                                (->> (map normalize-tag)
                                     (filter identity)
                                     set
                                     (map (fn [tag]
                                            [tag true]))
                                     (into {})))))))

(rf/reg-event-db :remove-tags
  (fn [db [_ db-path tags]]
    (update-in db db-path (fn [current-tags]
                            (loop [current-tags current-tags
                                   [tag & remaining] (->> tags
                                                          (map normalize-tag)
                                                          (filter identity)
                                                          set)]
                              (if tag
                                (recur (dissoc current-tags tag)
                                       remaining)
                                current-tags))))))

(rf/reg-event-db :toggle-tag
  (fn-traced [db [_ db-path tag]]
    (update-in db db-path (fn [current-tags]
                            (if (get current-tags tag)
                              (dissoc current-tags tag)
                              (assoc current-tags tag true))))))

(rf/reg-event-fx :clear-form-errors
  (fn-traced [_ [_ db-path]]
    {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]]}))

(rf/reg-event-fx :clear-form-message
  (fn-traced [_ [_ db-path]]
    {:fx [[:dispatch [:remove (into [:form-message] db-path)]]]}))

(rf/reg-event-fx :clear-form
  (fn-traced [_ [_ db-path]]
    {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]
          [:dispatch [:remove (into [:form-message] db-path)]]
          [:dispatch [:remove db-path]]]}))

(defn dispatch-on-event [event effect]
  (rf/dispatch effect)
  (.stopPropagation event))

(defn dispatch-on-event-sync [event effect]
  (rf/dispatch-sync effect)
  (.stopPropagation event))

(defn set-async-fetch-data [db-path query-id data]
  (rf/dispatch-sync [:set [:async-fetch-data db-path :current] query-id])
  (rf/dispatch-sync [:set [:async-fetch-data db-path :queries query-id] {:state :done
                                                                         :data data}])
  (rf/dispatch-sync [:set db-path data]))

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

(defn invalidate-cache-all []
  (rf/dispatch-sync [:set [:async-fetch-data] nil]))

(defn invalidate-cache-all-but-new []
  (rf/dispatch-sync [:update [:async-fetch-data]
                     (fn [async-data]
                       (->> async-data
                            (map (fn [[k v]]
                                   [k (-> v
                                          (update :queries select-keys [:new])
                                          (update :current #(when (= % :new) :new)))]))
                            (into {})))]))
