(ns heraldry.frontend.ui.element.charge-group-slot-number
  (:require [heraldry.frontend.ui.element.range :as range]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defmethod interface/form-element :charge-group-slot-number [path _]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui default min max]} option
          value (-> @(rf/subscribe [:get-value path])
                    count
                    (or 0))]
      [range/range-input nil
       :default default
       :min-value min
       :max-value max
       :step (or (:step ui) 1)
       :label (:label ui)
       :value value
       :on-change #(rf/dispatch [:set-charge-group-slot-number path %])])))
