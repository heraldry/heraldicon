(ns heraldry.frontend.ui.element.charge-group-slot-number
  (:require
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.element.range :as range]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
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

(defmethod ui-interface/form-element :charge-group-slot-number [{:keys [path] :as context}]
  (let [value (interface/get-list-size context)
        default (:default (interface/get-relevant-options context))]
    [range/range-input context
     :value value
     :on-change #(rf/dispatch [:set-charge-group-slot-number path (or % default)])]))
