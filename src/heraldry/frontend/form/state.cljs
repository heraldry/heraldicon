(ns heraldry.frontend.form.state
  (:require [clojure.walk :as walk]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.util :refer [deep-merge-with]]
            [re-frame.core :as rf]))


;; subs


(rf/reg-sub
 :ui-component-open?
 (fn [db [_ path]]
   (get-in db [:ui :component-open? path])))

(rf/reg-sub
 :ui-submenu-open?
 (fn [db [_ path]]
   (get-in db [:ui :submenu-open? path])))

(rf/reg-sub
 :ui-component-selected?
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


;; events


(rf/reg-event-db
 :ui-component-open
 (fn [db [_ path]]
   (-> (loop [db db
              rest path]
         (if (empty? rest)
           db
           (recur
            (assoc-in db [:ui :component-open? rest] true)
            (-> rest drop-last vec)))))))

(rf/reg-event-db
 :ui-component-close
 (fn [db [_ path]]

   (update-in db [:ui :component-open?]
              #(into {}
                     (->> %
                          (filter (fn [[k _]]
                                    (not (and (-> k count (>= (count path)))
                                              (= (subvec k 0 (count path))
                                                 path))))))))))

(rf/reg-event-fx
 :ui-component-open-toggle
 (fn [{:keys [db]} [_ path]]
   (let [open? (get-in db [:ui :component-open? path])]
     (if open?
       {:fx [[:dispatch [:ui-component-close path]]]}
       {:fx [[:dispatch [:ui-component-open path]]]}))))

(rf/reg-event-db
 :ui-component-deselect-all
 (fn [db _]
   (update-in db [:ui] dissoc :component-selected?)))

(rf/reg-event-db
 :ui-submenu-close-all
 (fn [db _]
   (update-in db [:ui] dissoc :submenu-open?)))

(rf/reg-event-db
 :ui-submenu-open
 (fn [db [_ path]]
   (assoc-in db [:ui :submenu-open? path] true)))

(rf/reg-event-db
 :ui-submenu-close
 (fn [db [_ path]]
   (assoc-in db [:ui :submenu-open? path] false)))

(rf/reg-event-fx
 :ui-component-select
 (fn [{:keys [db]} [_ path]]
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
              (update-in [:ui] dissoc :component-selected?)
              (cond->
               path (as-> db
                          (assoc-in db [:ui :component-selected? real-path] true))))
      :fx [[:dispatch [:ui-component-open real-path]]]})))

(rf/reg-event-db
 :prune-false-flags
 (fn [db [_ path]]
   (update-in db path (fn [flags]
                        (walk/postwalk (fn [value]
                                         (if (map? value)
                                           (->> value
                                                (filter #(-> % second (not= false)))
                                                (into {}))
                                           value))
                                       flags)))))

(rf/reg-event-db
 :set-field-type
 (fn [db [_ path new-type num-fields-x num-fields-y num-base-fields]]
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
         (update-in path dissoc :tincture)))))

(defn -default-line-style-of-ordinary-type [ordinary-type]
  (case ordinary-type
    :heraldry.ordinary.type/gore :enarched
    :straight))

(rf/reg-event-db
 :set-ordinary-type
 (fn [db [_ path new-type]]
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

(rf/reg-event-fx
 :add-component
 (fn [{:keys [db]} [_ path value]]
   (let [components-path (conj path :components)
         index (count (get-in db components-path))]
     {:db (update-in db components-path #(-> %
                                             (conj value)
                                             vec))
      :fx [[:dispatch [:ui-submenu-open (conj components-path index (case (-> value :type namespace)
                                                                      "heraldry.ordinary.type" "Select Ordinary"
                                                                      "heraldry.charge.type" "Select Charge"
                                                                      "heraldry.charge-group.type" nil
                                                                      "heraldry.component" "Select Semy"))]]
           [:dispatch [:ui-component-open (conj components-path index)]]
           [:dispatch [:ui-component-open (conj components-path index :field)]]]})))

(rf/reg-event-db
 :add-element
 (fn [db [_ path value]]
   (update-in db path (fn [elements]
                        (-> elements
                            (conj value)
                            vec)))))

(rf/reg-event-db
 :remove-element
 (fn [db [_ path]]
   (let [elements-path (drop-last path)
         index (last path)]
     (update-in db elements-path (fn [elements]
                                   (vec (concat (subvec elements 0 index)
                                                (subvec elements (inc index)))))))))

(rf/reg-event-db
 :move-element-up
 (fn [db [_ path]]
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

(rf/reg-event-db
 :move-element-down
 (fn [db [_ path]]
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

(rf/reg-event-db
 :update-charge
 (fn [db [_ path changes]]
   (update-in db path merge changes)))

(rf/reg-event-fx
 :add-arms-to-collection
 (fn [{:keys [db]} [_ path value index]]
   (let [elements-path (conj path :collection :elements)
         index (or index
                   (count (get-in db elements-path)))]
     {:db (update-in db elements-path #(let [before (take index %)
                                             after (drop index %)]
                                         (vec (concat before [value] after))))
      :fx [[:dispatch [:ui-submenu-open (conj elements-path index)]]
           [:dispatch [:ui-component-open (conj elements-path index)]]]})))

(rf/reg-event-db
 :cycle-charge-index
 (fn-traced [db [_ path num-charges]]
            (let [slots-path (drop-last path)
                  slot-index (last path)
                  slots (get-in db slots-path)
                  slots (if (-> slots count (<= slot-index))
                          (-> slots
                              (concat (repeat (-> slot-index inc (- (count slots))) nil))
                              vec)
                          slots)
                  current-value (get-in db path)
                  new-value (cond
                              (nil? current-value) 0
                              (= current-value (dec num-charges)) nil
                              (> current-value (dec num-charges)) 0
                              :else (inc current-value))]
              (assoc-in db slots-path (assoc slots slot-index new-value)))))

(rf/reg-event-db
 :remove-charge-group-charge
 (fn-traced [db [_ path]]
            (let [elements-path (drop-last path)
                  strips-path (-> path
                                  (->> (drop-last 2))
                                  vec
                                  (conj :strips))
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
                                                 strips)))))))
