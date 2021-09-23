(ns heraldry.frontend.ui.element.range
  (:require [heraldry.frontend.language :refer [tr]]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn range-input [_]
  (let [tmp-value (r/atom nil)
        focused? (r/atom false)]
    (fn [path & {:keys [value on-change disabled?]}]
      (when-let [option @(rf/subscribe [:get-relevant-options path])]
        (let [component-id (util/id "range")
              current-value @(rf/subscribe [:get-value path])
              {:keys [ui inherited default min max]} option
              step (or (:step ui) 1)
              label (:label ui)
              tooltip (:tooltip ui)
              value (or value
                        current-value
                        inherited
                        default
                        min)]
          [:div.ui-setting
           [:label {:for component-id} [tr label]
            (when tooltip
              [:div.tooltip.info {:style {:display "inline-block"
                                          :margin-left "0.2em"}}
               [:i.fas.fa-question-circle]
               [:div.bottom
                [:h3 {:style {:text-align "center"}} tooltip]
                [:i]]])]
           [:div.option
            [:input {:type "range"
                     :id component-id
                     :min min
                     :max max
                     :step step
                     :value value
                     :disabled disabled?
                     :on-change #(let [value (-> % .-target .-value js/parseFloat)
                                       value (if (js/isNaN value)
                                               nil
                                               value)]
                                   (if on-change
                                     (on-change value)
                                     (rf/dispatch [:set path value])))
                     :style {:width "10em"}}]
            [:input {:type "text"
                     :value (if @focused?
                              @tmp-value
                              value)
                     :on-focus #(do
                                  (swap! tmp-value (fn [_] (str value)))
                                  (swap! focused? (fn [_] true)))
                     :on-change (when @focused?
                                  #(swap! tmp-value (fn [_] (-> % .-target .-value))))
                     :on-blur #(let [parsed-value (js/parseFloat @tmp-value)]
                                 (when-not (js/isNaN parsed-value)
                                   (if on-change
                                     (on-change parsed-value)
                                     (rf/dispatch-sync [:set path parsed-value])))
                                 (swap! focused? (fn [_] false)))
                     :style {:display "inline-block"
                             :width "2em"
                             :margin-left "0.5em"}}]
            [value-mode-select/value-mode-select path
             :disabled? disabled?
             :on-change on-change]]])))))

(defmethod interface/form-element :range [path]
  [range-input path])
