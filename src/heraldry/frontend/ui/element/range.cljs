(ns heraldry.frontend.ui.element.range
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.hover-menu :as hover-menu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn range-input [path & {:keys [value on-change default step
                                  disabled? tooltip label
                                  min-value max-value]}]
  (let [component-id (util/id "range")
        current-value @(rf/subscribe [:get-value path])
        value (or value
                  current-value
                  default
                  min-value)
        menu (cond-> []
               default (conj {:title "Auto"
                              :icon (if current-value
                                      "far fa-square"
                                      "far fa-check-square")
                              :handler #(state/dispatch-on-event % [:set path nil])}))
        menu (cond-> menu
               (seq menu) (conj {:title "Manual"
                                 :icon (if current-value
                                         "far fa-check-square"
                                         "far fa-square")
                                 :handler #(state/dispatch-on-event % [:set path value])}))]

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
      (when (seq menu)
        [:div {:style {:display "inline-block"
                       :margin-left "0.5em"
                       :position "absolute"}}
         [hover-menu/hover-menu
          path
          "Mode"
          menu
          [:i.ui-icon {:class "fas fa-cog"}]
          :disabled? disabled?]])]]))

(defmethod interface/form-element :range [path _]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui default min max]} option]
      [range-input path
       :default default
       :min-value min
       :max-value max
       :step (or (:step ui) 1)
       :label (:label ui)])))
