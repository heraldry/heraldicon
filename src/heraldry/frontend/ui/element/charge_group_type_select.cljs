(ns heraldry.frontend.ui.element.charge-group-type-select
  (:require [heraldry.frontend.ui.element.radio-select :as radio-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.macros :as macros]
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

(defmethod interface/form-element :charge-group-type-select [path]
  [radio-select/radio-select path
   :on-change #(rf/dispatch [:change-charge-group-type (vec (drop-last path)) %])])
