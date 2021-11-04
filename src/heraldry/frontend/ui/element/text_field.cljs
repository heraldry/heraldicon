(ns heraldry.frontend.ui.element.text-field
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [re-frame.core :as rf]))

(defn text-field [{:keys [path] :as context} & {:keys [on-change style]}]
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
                                 (rf/dispatch-sync [:set path value])))}]]])))

(defmethod ui-interface/form-element :text-field [context]
  [text-field context])
