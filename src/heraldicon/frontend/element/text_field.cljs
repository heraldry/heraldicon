(ns heraldicon.frontend.element.text-field
  (:require
   [heraldicon.frontend.interface :as ui.interface]
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
          label (:label ui)]
      [:div.ui-setting
       {:style style}
       (when label
         [:label [tr label]])
       [:div.option
        [:input {:type "text"
                 :value value
                 :on-change #(let [value (-> % .-target .-value)]
                               (if on-change
                                 (on-change value)
                                 (rf/dispatch-sync [:set context value])))}]]])))

(defmethod ui.interface/form-element :text-field [context]
  [text-field context])