(ns heraldry.frontend.ui.element.range
  (:require [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn range-input [path & {:keys [on-change inherited default step
                                  disabled? tooltip label
                                  min-value max-value]}]
  (let [component-id (util/id "range")
        current-value @(rf/subscribe [:get-value path])
        value (or current-value
                  inherited
                  default
                  min-value)]

    [:div.ui-setting
     [:label {:for component-id} label]
     (when tooltip
       [:div.tooltip.info {:style {:display "inline-block"
                                   :margin-left "0.2em"}}
        [:i.fas.fa-question-circle]
        [:div.bottom
         [:h3 {:style {:text-align "center"}} tooltip]
         [:i]]])
     [:div.option
      [:input {:type "range"
               :id component-id
               :min min-value
               :max max-value
               :step step
               :value value
               :disabled disabled?
               :on-change #(let [value (-> % .-target .-value js/parseFloat)]
                             (if on-change
                               (on-change value)
                               (rf/dispatch [:set path value])))}]
      [:input {:type "text"
               :value value
               :on-change #(let [value (-> % .-target .-value)]
                             (if on-change
                               (on-change value)
                               (rf/dispatch-sync [:set path value])))
               :style {:display "inline-block"
                       :width "2em"
                       :margin-left "0.5em"}}]
      [value-mode-select/value-mode-select
       path
       :value current-value
       :inherited inherited
       :default default
       :disabled? disabled?]]]))

(defmethod interface/form-element :range [path _]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui inherited default min max]} option]
      [range-input path
       :default default
       :inherited inherited
       :min-value min
       :max-value max
       :step (or (:step ui) 1)
       :label (:label ui)])))
