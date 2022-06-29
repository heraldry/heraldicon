(ns heraldicon.frontend.element.charge-group-slot-number
  (:require
   [heraldicon.frontend.element.range :as range]
   [heraldicon.frontend.interface :as ui.interface]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(macros/reg-event-db :set-charge-group-slot-number
  (fn [db [_ path num-slots]]
    (update-in db path (fn [slots]
                         (if (-> slots count (< num-slots))
                           (-> slots
                               (concat (repeat (- num-slots (count slots)) 0))
                               vec)
                           (->> slots
                                (take num-slots)
                                vec))))))

(defmethod ui.interface/form-element :charge-group-slot-number [{:keys [path] :as context}]
  (let [value (interface/get-list-size context)
        default (:default (interface/get-relevant-options context))]
    [range/range-input context
     :value value
     :on-change #(rf/dispatch [:set-charge-group-slot-number path (or % default)])]))
