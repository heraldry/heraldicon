(ns heraldry.frontend.state
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.attributes :as attributes]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.field.core :as field]
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

(rf/reg-event-db :update-charge
  (fn [db [_ path changes]]
    (update-in db path merge changes)))

(rf/reg-event-db :cycle-charge-index
  (fn [db [_ path num-charges]]
    (let [slots-path (drop-last path)
          slot-index (last path)
          slots (get-in db slots-path)
          current-value (get-in db path)
          new-value (cond
                      (nil? current-value) 0
                      (= current-value (dec num-charges)) nil
                      (> current-value (dec num-charges)) 0
                      :else (inc current-value))]
      (assoc-in db slots-path (assoc slots slot-index new-value)))))

(rf/reg-event-db :remove-charge-group-charge
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-path (-> path
                          (->> (drop-last 2))
                          vec
                          (conj :strips))
          slots-path (-> path
                         (->> (drop-last 2))
                         vec
                         (conj :slots))
          index (last path)]
      (-> db
          (update-in elements-path (fn [elements]
                                     (vec (concat (subvec elements 0 index)
                                                  (subvec elements (inc index))))))
          (update-in strips-path (fn [strips]
                                   (mapv (fn [strip]
                                           (update strip :slots (fn [slots]
                                                                  (mapv (fn [charge-index]
                                                                          (cond
                                                                            (= charge-index index) 0
                                                                            (> charge-index index) (dec charge-index)
                                                                            :else charge-index))
                                                                        slots))))
                                         strips)))
          (update-in slots-path (fn [slots]
                                  (mapv (fn [charge-index]
                                          (cond
                                            (= charge-index index) 0
                                            (> charge-index index) (dec charge-index)
                                            :else charge-index))
                                        slots)))))))

(rf/reg-event-db :move-charge-group-charge-up
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-path (-> path
                          (->> (drop-last 2))
                          vec
                          (conj :strips))
          slots-path (-> path
                         (->> (drop-last 2))
                         vec
                         (conj :slots))
          index (last path)]
      (-> db
          (update-in elements-path (fn [elements]
                                     (let [num-elements (count elements)]
                                       (if (>= index num-elements)
                                         elements
                                         (-> elements
                                             (subvec 0 index)
                                             (conj (get elements (inc index)))
                                             (conj (get elements index))
                                             (concat (subvec elements (+ index 2)))
                                             vec)))))
          (update-in strips-path (fn [strips]
                                   (mapv (fn [strip]
                                           (update strip :slots (fn [slots]
                                                                  (mapv (fn [charge-index]
                                                                          (cond
                                                                            (= charge-index index) (inc charge-index)
                                                                            (= charge-index (inc index)) (dec charge-index)
                                                                            :else charge-index))
                                                                        slots))))
                                         strips)))
          (update-in slots-path (fn [slots]
                                  (mapv (fn [charge-index]
                                          (cond
                                            (= charge-index index) (inc charge-index)
                                            (= charge-index (inc index)) (dec charge-index)
                                            :else charge-index))
                                        slots)))))))

(rf/reg-event-db :move-charge-group-charge-down
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-path (-> path
                          (->> (drop-last 2))
                          vec
                          (conj :strips))
          slots-path (-> path
                         (->> (drop-last 2))
                         vec
                         (conj :slots))
          index (last path)]
      (-> db
          (update-in elements-path (fn [elements]
                                     (if (zero? index)
                                       elements
                                       (-> elements
                                           (subvec 0 (dec index))
                                           (conj (get elements index))
                                           (conj (get elements (dec index)))
                                           (concat (subvec elements (inc index)))
                                           vec))))
          (update-in strips-path (fn [strips]
                                   (mapv (fn [strip]
                                           (update strip :slots (fn [slots]
                                                                  (mapv (fn [charge-index]
                                                                          (cond
                                                                            (= charge-index (dec index)) (inc charge-index)
                                                                            (= charge-index index) (dec charge-index)
                                                                            :else charge-index))
                                                                        slots))))
                                         strips)))
          (update-in slots-path (fn [slots]
                                  (mapv (fn [charge-index]
                                          (cond
                                            (= charge-index (dec index)) (inc charge-index)
                                            (= charge-index index) (dec charge-index)
                                            :else charge-index))
                                        slots)))))))

(rf/reg-event-db :set-charge-group-slot-number
  (fn [db [_ path num-slots]]
    (-> db
        (update-in path (fn [slots]
                          (if (-> slots count (< num-slots))
                            (-> slots
                                (concat (repeat (- num-slots (count slots)) 0))
                                vec)
                            (->> slots
                                 (take num-slots)
                                 vec)))))))
(rf/reg-event-db :change-charge-group-type
  (fn [db [_ path new-type]]
    (-> db
        (update-in path (fn [charge-group]
                          (-> charge-group
                              (assoc :type new-type)
                              (cond->
                               (and (-> new-type
                                        #{:heraldry.charge-group.type/rows
                                          :heraldry.charge-group.type/columns})
                                    (-> charge-group :strips not)) (assoc :strips [{:slots [0 0]}
                                                                                   {:slots [0]}])
                               (and (-> new-type
                                        (= :heraldry.charge-group.type/arc))
                                    (-> charge-group :slots not)) (assoc :slots [0 0 0 0 0]))))))))

(rf/reg-event-db :select-charge-group-preset
  ;; TODO: this must not be an fn, can be done once
  ;; https://github.com/day8/re-frame-debux/issues/40 is resolved
  (fn [db [_ path charge-group-preset charge-adjustments]]
    (let [new-db (-> db
                     (update-in path (fn [charge-group]
                                       (-> charge-group-preset
                                           (assoc :charges (:charges charge-group)))))
                     (assoc-in (conj path :charges 0 :anchor :point) :angle)
                     (assoc-in (conj path :charges 0 :anchor :angle) 0)
                     (assoc-in (conj path :charges 0 :geometry :size) nil))]
      (loop [new-db new-db
             [[rel-path value] & rest] charge-adjustments]
        (if (not rel-path)
          new-db
          (recur
           (assoc-in new-db (concat path rel-path) value)
           rest))))))

(rf/reg-event-fx :override-field-part-reference
  (fn [{:keys [db]} [_ path]]
    (let [{:keys [index]} (get-in db path)
          referenced-part (get-in db (-> path
                                         drop-last
                                         vec
                                         (conj index)))]
      {:db (assoc-in db path referenced-part)
       :dispatch [:ui-component-node-select path {:open? true}]})))

(rf/reg-event-fx :reset-field-part-reference
  (fn [{:keys [db]} [_ path]]
    (let [index (last path)
          parent (get-in db (drop-last 2 path))]
      {:db (assoc-in db path (-> (field/default-fields parent)
                                 (get index)))})))

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
