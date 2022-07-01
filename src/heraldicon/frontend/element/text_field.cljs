(ns heraldicon.frontend.element.text-field
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defn text-field [context & {:keys [on-change style]}]
  (when-let [option (interface/get-relevant-options context)]
    (let [{:keys [ui inherited default]} option
          current-value (interface/get-raw-data context)
          value (or current-value
                    inherited
                    default)
          {:keys [label tooltip]} ui]
      [:div.ui-setting
       {:style style}
       (when label
         [:label [tr label]
          (when tooltip
            [:div.tooltip.info {:style {:display "inline-block"
                                        :margin-left "0.2em"}}
             [:i.fas.fa-question-circle]
             [:div.bottom
              [:h3 {:style {:text-align "center"}} [tr tooltip]]
              [:i]]])])
       [:div.option
        [:input {:type "text"
                 :value value
                 :on-change #(let [value (-> % .-target .-value)]
                               (if on-change
                                 (on-change value)
                                 (rf/dispatch-sync [:set context value])))}]]])))

(defmethod element/element :text-field [context]
  [text-field context])
