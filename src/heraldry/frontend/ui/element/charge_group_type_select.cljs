(ns heraldry.frontend.ui.element.charge-group-type-select
  (:require
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.element.radio-select :as radio-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [re-frame.core :as rf]))

(macros/reg-event-db :change-charge-group-type
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

(defmethod ui-interface/form-element :charge-group-type-select [{:keys [path] :as context}]
  [radio-select/radio-select context
   :on-change #(rf/dispatch [:change-charge-group-type (vec (drop-last path)) %])])
