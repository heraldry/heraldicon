(ns heraldicon.frontend.element.text-field
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]))

(defn text-field [context & {:keys [on-change style]}]
  (when-let [option (interface/get-options context)]
    (let [{:keys [inherited default]
           :ui/keys [label tooltip placeholder]} option
          current-value (interface/get-raw-data context)
          value (or current-value
                    inherited
                    default)]
      [:div.ui-setting
       {:style style}
       (when label
         [:label [tr label]
          [tooltip/info tooltip]])
       [:div.option
        [:input {:type "text"
                 :value value
                 :placeholder (tr placeholder)
                 :on-change #(let [value (-> % .-target .-value)]
                               (if on-change
                                 (on-change value)
                                 (rf/dispatch-sync [:set context value])))}]]])))

(defmethod element/element :ui.element/text-field [context]
  [text-field context])
