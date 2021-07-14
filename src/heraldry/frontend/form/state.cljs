(ns heraldry.frontend.form.state
  (:require [clojure.walk :as walk]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.util :refer [deep-merge-with]]
            [re-frame.core :as rf]))

;; subs

(def node-flag-db-path [:ui :component-tree :nodes])
(def ui-component-node-selected-path [:ui :component-tree :selected-node])

(rf/reg-sub :ui-component-open?
  (fn [db [_ path]]
    (get-in db [:ui :component-open? path])))

(rf/reg-sub :ui-submenu-open?
  (fn [db [_ path]]
    (get-in db [:ui :submenu-open? path])))

(rf/reg-sub :ui-hover-menu-open?
  (fn [db [_ path]]
    (get-in db [:ui :hover-menu-open? path])))

(rf/reg-sub :ui-component-selected?
  (fn [db [_ path]]
    (or (get-in db [:ui :component-selected? path])
        (when (get-in db (-> path
                             (->> (drop-last 3))
                             vec
                             (conj :counterchanged?)))
          (let [parent-field-path (-> path
                                      (->> (drop-last 6))
                                      vec
                                      (conj :fields (last path)))]
            (get-in db [:ui :component-selected? parent-field-path]))))))

(rf/reg-sub :ui-component-node-open?
  (fn [[_ path] _]
    (rf/subscribe [:get (conj node-flag-db-path path)]))

  (fn [flag [_ _path]]
    flag))

(rf/reg-sub :ui-component-node-selected-path
  (fn [_ _]
    (rf/subscribe [:get ui-component-node-selected-path]))

  (fn [selected-node-path [_ _path]]
    selected-node-path))

;; events

(rf/reg-event-db :ui-component-open
  (fn [db [_ path]]
    (let [db-after-closing (assoc-in db [:ui :component-open?] {})]
      (loop [db db-after-closing
             rest path]
        (if (empty? rest)
          db
          (recur
           (assoc-in db [:ui :component-open? rest] true)
           (-> rest drop-last vec)))))))

(rf/reg-event-db :ui-component-close
  (fn-traced [db [_ path]]

    (update-in db [:ui :component-open?]
               #(into {}
                      (->> %
                           (filter (fn [[k _]]
                                     (not (and (-> k count (>= (count path)))
                                               (= (subvec k 0 (count path))
                                                  path))))))))))

(rf/reg-event-fx :ui-component-open-toggle
  (fn-traced [{:keys [db]} [_ path]]
    (let [open? (get-in db [:ui :component-open? path])]
      (if open?
        {:fx [[:dispatch [:ui-component-close path]]]}
        {:fx [[:dispatch [:ui-component-open path]]]}))))

(rf/reg-event-db :ui-component-deselect-all
  (fn-traced [db _]
    (update db :ui dissoc :component-selected?)))

(rf/reg-event-db :ui-submenu-close-all
  (fn-traced [db _]
    (update db :ui dissoc :submenu-open?)))

(rf/reg-event-db :ui-submenu-open
  (fn-traced [db [_ path]]
    (-> db
        (update-in [:ui :submenu-open?]
                   (fn [open-flags]
                     (->> open-flags
                          (keep (fn [[key value]]
                                  (when (= key
                                           (take (count key) path))
                                    [key value])))
                          (into {}))))
        (assoc-in [:ui :submenu-open? path] true))))

(rf/reg-event-db :ui-submenu-close
  (fn-traced [db [_ path]]
    (assoc-in db [:ui :submenu-open? path] false)))

(rf/reg-event-db :ui-hover-menu-open
  (fn-traced [db [_ path]]
    (assoc-in db [:ui :hover-menu-open? path] true)))

(rf/reg-event-db :ui-hover-menu-close
  (fn-traced [db [_ path]]
    (update-in db [:ui :hover-menu-open?] dissoc path)))

(rf/reg-event-fx :ui-component-select
  (fn-traced [{:keys [db]} [_ path]]
    (let [real-path (if (get-in
                         db
                         (-> path
                             (->> (drop-last 3))
                             vec
                             (conj :counterchanged?)))
                      (-> path
                          (->> (drop-last 6))
                          vec
                          (conj :fields (last path)))
                      path)]
      {:db (-> db
               (update :ui dissoc :component-selected?)
               (cond->
                path (as-> db
                           (assoc-in db [:ui :component-selected? real-path] true))))
       :fx [[:dispatch [:ui-component-open real-path]]]})))

(rf/reg-event-db :ui-component-node-open
  (fn-traced [db [_ path]]
    (let [path (vec path)]
      (->> (range (count path))
           (map (fn [idx]
                  [(subvec path 0 (inc idx)) true]))
           (into {})
           (update-in db node-flag-db-path merge)))))

(rf/reg-event-db :ui-component-node-close
  (fn-traced [db [_ path]]
    (update-in
     db node-flag-db-path
     (fn [flags]
       (->> flags
            (filter (fn [[other-path v]]
                      (when (not= (take (count path) other-path)
                                  path)
                        [other-path v])))
            (into {}))))))

(rf/reg-event-fx :ui-component-node-toggle
  (fn-traced [{:keys [db]} [_ path]]
    {:dispatch (if (get-in db (conj node-flag-db-path path))
                 [:ui-component-node-close path]
                 [:ui-component-node-open path])}))

(rf/reg-event-fx :ui-component-node-select
  (fn-traced [{:keys [db]} [_ path {:keys [open?]}]]
    {:db (assoc-in db ui-component-node-selected-path path)
     :dispatch [:ui-component-node-open (cond-> path
                                          (not open?) drop-last)]}))

(rf/reg-event-fx :ui-component-node-select-default
  (fn-traced [{:keys [db]} [_ path]]
    (let [old-path (get-in db ui-component-node-selected-path)
          new-path (if (or (not old-path)
                           (not= (subvec old-path 0 (count path))
                                 path))
                     path
                     old-path)]
      (cond-> {}
        (not= new-path
              old-path) (assoc :dispatch [:ui-component-node-select new-path])))))

(rf/reg-event-db :prune-false-flags
  (fn-traced [db [_ path]]
    (update-in db path (fn [flags]
                         (walk/postwalk (fn [value]
                                          (if (map? value)
                                            (->> value
                                                 (filter #(-> % second (not= false)))
                                                 (into {}))
                                            value))
                                        flags)))))

(rf/reg-event-db :set-field-type
  (fn-traced [db [_ path new-type num-fields-x num-fields-y num-base-fields]]
    (let [path (vec path)]
      (if (= new-type :heraldry.field.type/plain)
        (-> db
            (assoc-in (conj path :type) new-type)
            (update-in (conj path :tincture) #(or % :none)))
        (-> db
            (assoc-in (conj path :type) new-type)
            (update-in (conj path :line :type) #(or % :straight))
            (assoc-in (conj path :layout :num-fields-x) num-fields-x)
            (assoc-in (conj path :layout :num-fields-y) num-fields-y)
            (assoc-in (conj path :layout :num-base-fields) num-base-fields)
            (update-in path
                       (fn [prepared-field]
                         (let [current (or (:fields prepared-field) [])
                               default (field/default-fields prepared-field)
                               previous-default (field/default-fields (get-in db path))
                               previous-default (cond
                                                  (< (count previous-default) (count default)) (into previous-default (subvec default (count previous-default)))
                                                  (> (count previous-default) (count default)) (subvec previous-default 0 (count default))
                                                  :else previous-default)
                               merged (cond
                                        (< (count current) (count default)) (into current (subvec default (count current)))
                                        (> (count current) (count default)) (subvec current 0 (count default))
                                        :else current)]
                           (-> prepared-field
                               (assoc :fields (->> (map vector merged previous-default default)
                                                   (map (fn [[cur old-def def]]
                                                          (if (and (-> cur
                                                                       :type
                                                                       (not= :heraldry.field.type/ref))
                                                                   (not= cur old-def))
                                                            cur
                                                            def)))
                                                   vec))))))
            (update-in path #(merge %
                                    (options/sanitize-or-nil % (field-options/options %))))
            (update-in path dissoc :tincture))))))

(defn -default-line-style-of-ordinary-type [ordinary-type]
  (case ordinary-type
    :heraldry.ordinary.type/gore :enarched
    :straight))

(rf/reg-event-db :set-ordinary-type
  (fn-traced [db [_ path new-type]]
    (let [current (get-in db path)
          has-default-line-style? (-> current
                                      :line
                                      :type
                                      (= (-default-line-style-of-ordinary-type (:type current))))
          new-default-line-style (-default-line-style-of-ordinary-type new-type)
          new-flipped (case new-type
                        :heraldry.ordinary.type/gore true
                        false)]
      (-> db
          (assoc-in (conj path :type) new-type)
          (cond->
           has-default-line-style? (->
                                    (assoc-in (conj path :line :type) new-default-line-style)
                                    (assoc-in (conj path :line :flipped?) new-flipped)))
          (update-in path #(deep-merge-with (fn [_current-value new-value]
                                              new-value)
                                            %
                                            (options/sanitize-or-nil % (ordinary-options/options %))))))))

(rf/reg-event-fx :add-component
  (fn-traced [{:keys [db]} [_ path value]]
    (let [components-path (conj path :components)
          index (count (get-in db components-path))]
      {:db (update-in db components-path #(-> %
                                              (conj value)
                                              vec))
       :dispatch [:ui-component-node-select (conj components-path index) {:open? true}]})))

(rf/reg-event-fx :add-element
  (fn-traced [{:keys [db]} [_ path value]]
    (let [elements (-> (get-in db path)
                       (conj value)
                       vec)
          element-path (conj path (-> elements count dec))]
      {:db (assoc-in db path elements)
       :dispatch [:ui-component-node-select element-path {:open? true}]})))

(rf/reg-event-db :remove-element
  (fn-traced [db [_ path]]
    (let [elements-path (drop-last path)
          index (last path)]
      (update-in db elements-path (fn [elements]
                                    (vec (concat (subvec elements 0 index)
                                                 (subvec elements (inc index)))))))))

(rf/reg-event-db :move-element-up
  (fn-traced [db [_ path]]
    (let [elements-path (drop-last path)
          index (last path)]
      (update-in db elements-path (fn [elements]
                                    (let [num-elements (count elements)]
                                      (if (>= index num-elements)
                                        elements
                                        (-> elements
                                            (subvec 0 index)
                                            (conj (get elements (inc index)))
                                            (conj (get elements index))
                                            (concat (subvec elements (+ index 2)))
                                            vec))))))))

(rf/reg-event-db :move-element-down
  (fn-traced [db [_ path]]
    (let [elements-path (drop-last path)
          index (last path)]
      (update-in db elements-path (fn [elements]
                                    (if (zero? index)
                                      elements
                                      (-> elements
                                          (subvec 0 (dec index))
                                          (conj (get elements index))
                                          (conj (get elements (dec index)))
                                          (concat (subvec elements (inc index)))
                                          vec)))))))

(rf/reg-event-db :update-charge
  (fn-traced [db [_ path changes]]
    (update-in db path merge changes)))

(rf/reg-event-fx :add-arms-to-collection
  (fn-traced [{:keys [db]} [_ path value index]]
    (let [elements-path (conj path :collection :elements)
          index (or index
                    (count (get-in db elements-path)))]
      {:db (update-in db elements-path #(let [before (take index %)
                                              after (drop index %)]
                                          (vec (concat before [value] after))))
       :fx [[:dispatch [:ui-submenu-open (conj elements-path index)]]
            [:dispatch [:ui-component-open (conj elements-path index)]]]})))

(rf/reg-event-db :cycle-charge-index
  (fn-traced [db [_ path num-charges]]
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
  (fn-traced [db [_ path]]
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
  (fn-traced [db [_ path]]
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
  (fn-traced [db [_ path]]
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
  (fn-traced [db [_ path num-slots]]
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
  (fn-traced [db [_ path new-type]]
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
  ;; TODO: this must not be an fn-traced, can be done once
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
  (fn-traced [{:keys [db]} [_ path]]
    (let [{:keys [index]} (get-in db path)
          referenced-part (get-in db (-> path
                                         drop-last
                                         vec
                                         (conj index)))]
      {:db (assoc-in db path referenced-part)
       :dispatch [:ui-component-node-select path {:open? true}]})))

(rf/reg-event-fx :reset-field-part-reference
  (fn-traced [{:keys [db]} [_ path]]
    (let [index (last path)
          parent (get-in db (drop-last 2 path))]
      {:db (assoc-in db path (-> (field/default-fields parent)
                                 (get index)))})))

(rf/reg-event-fx :set-field-layout-num-fields-x
  (fn-traced [{:keys [db]} [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      {:dispatch [:set-field-type
                  field-path
                  (:type field)
                  value
                  (-> field :layout :num-fields-y)
                  (-> field :layout :num-base-fields)]})))

(rf/reg-event-fx :set-field-layout-num-fields-y
  (fn-traced [{:keys [db]} [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      {:dispatch [:set-field-type
                  field-path
                  (:type field)
                  (-> field :layout :num-fields-x)
                  value
                  (-> field :layout :num-base-fields)]})))

(rf/reg-event-fx :set-field-layout-num-base-fields
  (fn-traced [{:keys [db]} [_ path value]]
    (let [field-path (drop-last 2 path)
          field (get-in db field-path)]
      {:dispatch [:set-field-type
                  field-path
                  (:type field)
                  (-> field :layout :num-fields-x)
                  (-> field :layout :num-fields-y)
                  value]})))
