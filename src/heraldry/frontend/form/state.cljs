(ns heraldry.frontend.form.state
  (:require [clojure.walk :as walk]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.coat-of-arms.field.core :as division]
            [heraldry.coat-of-arms.division.options :as division-options]
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
   (-> (loop [db   db
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
 :set-division-type
 (fn [db [_ path new-type num-fields-x num-fields-y num-base-fields]]
   (if (= new-type :plain)
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
                    (fn [prepared-division]
                      (let [current          (or (:fields prepared-division) [])
                            default          (division/default-fields prepared-division)
                            previous-default (division/default-fields (get-in db path))
                            previous-default (cond
                                               (< (count previous-default) (count default)) (into previous-default (subvec default (count previous-default)))
                                               (> (count previous-default) (count default)) (subvec previous-default 0 (count default))
                                               :else                                        previous-default)
                            merged           (cond
                                               (< (count current) (count default)) (into current (subvec default (count current)))
                                               (> (count current) (count default)) (subvec current 0 (count default))
                                               :else                               current)]
                        (-> prepared-division
                            (assoc :fields (->> (map vector merged previous-default default)
                                                (map (fn [[cur old-def def]]
                                                       (if (and (-> cur :ref not)
                                                                (not= cur old-def))
                                                         cur
                                                         def)))
                                                vec))))))
         (update-in path #(merge %
                                 (options/sanitize-or-nil % (division-options/options %))))
         (update-in path dissoc :tincture)))))

(defn -default-line-style-of-ordinary-type [ordinary-type]
  (case ordinary-type
    :gore :enarched
    :straight))

(rf/reg-event-db
 :set-ordinary-type
 (fn [db [_ path new-type]]
   (let [current                 (get-in db path)
         has-default-line-style? (-> current
                                     :line
                                     :type
                                     (= (-default-line-style-of-ordinary-type (:type current))))
         new-default-line-style  (-default-line-style-of-ordinary-type new-type)
         new-flipped             (case new-type
                                   :gore true
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
(rf/reg-event-db
 :set-charge-type
 (fn [db [_ path new-type]]
   (-> db
       (assoc-in (conj path :type) new-type)
       (update-in path #(deep-merge-with (fn [_current-value new-value]
                                           new-value)
                                         %
                                         (options/sanitize-or-nil % (charge-options/options %)))))))

(rf/reg-event-fx
 :add-component
 (fn [{:keys [db]} [_ path value]]
   (let [components-path (conj path :components)
         index           (count (get-in db components-path))]
     {:db (update-in db components-path #(-> %
                                             (conj value)
                                             vec))
      :fx [[:dispatch [:ui-submenu-open (conj components-path index (case (:component value)
                                                                      :ordinary "Select Ordinary"
                                                                      :charge   "Select Charge"))]]
           [:dispatch [:ui-component-open (conj components-path index)]]
           [:dispatch [:ui-component-open (conj components-path index :field)]]]})))

(rf/reg-event-db
 :remove-component
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index           (last path)]
     (update-in db components-path (fn [components]
                                     (vec (concat (subvec components 0 index)
                                                  (subvec components (inc index)))))))))

(rf/reg-event-db
 :move-component-up
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index           (last path)]
     (update-in db components-path (fn [components]
                                     (let [num-components (count components)]
                                       (if (>= index num-components)
                                         components
                                         (-> components
                                             (subvec 0 index)
                                             (conj (get components (inc index)))
                                             (conj (get components index))
                                             (concat (subvec components (+ index 2)))
                                             vec))))))))

(rf/reg-event-db
 :move-component-down
 (fn [db [_ path]]
   (let [components-path (drop-last path)
         index           (last path)]
     (update-in db components-path (fn [components]
                                     (if (zero? index)
                                       components
                                       (-> components
                                           (subvec 0 (dec index))
                                           (conj (get components index))
                                           (conj (get components (dec index)))
                                           (concat (subvec components (inc index)))
                                           vec)))))))

(rf/reg-event-db
 :update-charge
 (fn [db [_ path changes]]
   (update-in db path merge changes)))

