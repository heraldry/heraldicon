(ns heraldry.frontend.ui.element.range
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn range-input [path & {:keys [value on-change default display-function step
                                  disabled? tooltip label
                                  min-value max-value]}]
  (let [component-id (util/id "range")
        current-value @(rf/subscribe [:get-value path])
        value (or value
                  current-value
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
      [:span {:style {:margin-left "1em"}} (cond-> value
                                             display-function display-function)]]]))

(defmethod interface/form-element :range [path {:keys [ui default min max] :as option}]
  (when option
    [range-input path
     :default default
     :min-value min
     :max-value max
     :step (or (:step ui) 1)
     :label (:label ui)]))
