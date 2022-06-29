(ns heraldicon.frontend.element.charge-group-type-select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.radio-select :as radio-select]
   [heraldicon.frontend.macros :as macros]
   [re-frame.core :as rf]))

(macros/reg-event-db :change-charge-group-type
  (fn [db [_ path new-type]]
    (update-in db path (fn [charge-group]
                         (-> charge-group
                             (assoc :type new-type)
                             (cond->
                               (and (#{:heraldry.charge-group.type/rows
                                       :heraldry.charge-group.type/columns}
                                     new-type)
                                    (-> charge-group :strips not)) (assoc :strips [{:slots [0 0]}
                                                                                   {:slots [0]}])
                               (and (= :heraldry.charge-group.type/arc
                                       new-type)
                                    (-> charge-group :slots not)) (assoc :slots [0 0 0 0 0])
                               (and (= :heraldry.charge-group.type/in-orle
                                       new-type)
                                    (-> charge-group :slots not)) (assoc :slots [0 0 0 0 0 0 0 0])))))))

(defmethod element/element :charge-group-type-select [{:keys [path] :as context}]
  [radio-select/radio-select context
   :on-change #(rf/dispatch [:change-charge-group-type (vec (drop-last path)) %])])
