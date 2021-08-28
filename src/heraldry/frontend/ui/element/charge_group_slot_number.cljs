(ns heraldry.frontend.ui.element.charge-group-slot-number
  (:require [heraldry.frontend.ui.element.range :as range]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.macros :as macros]
            [re-frame.core :as rf]))

(macros/reg-event-db :set-charge-group-slot-number
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

(defmethod interface/form-element :charge-group-slot-number [path]
  (let [value @(rf/subscribe [:get-list-size path])
        default (:default @(rf/subscribe [:get-relevant-options path]))]
    [range/range-input path
     :value value
     :on-change #(rf/dispatch [:set-charge-group-slot-number path (or % default)])]))
