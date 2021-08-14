(ns heraldry.frontend.ui.element.text-field
  (:require [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn text-field [path & {:keys [on-change style]}]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui inherited default]} option
          current-value @(rf/subscribe [:get-value path])
          value (or current-value
                    inherited
                    default)
          label (:label ui)]
      [:div.ui-setting
       {:style style}
       (when label
         [:label label])
       [:div.option
        [:input {:type "text"
                 :value value
                 :on-change #(let [value (-> % .-target .-value)]
                               (if on-change
                                 (on-change value)
                                 (rf/dispatch-sync [:set path value])))}]]])))

(defmethod interface/form-element :text-field [path]
  [text-field path])

