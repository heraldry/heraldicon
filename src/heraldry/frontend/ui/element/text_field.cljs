(ns heraldry.frontend.ui.element.text-field
  (:require [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn text-field [path & {:keys [default label on-change]}]
  (let [value (or @(rf/subscribe [:get-value path])
                  default)]
    [:div.ui-setting
     (when label
       [:label label])
     [:div.option
      [:input {:type "text"
               :value value
               :on-change #(let [value (-> % .-target .-value)]
                             (if on-change
                               (on-change value)
                               (rf/dispatch-sync [:set path value])))}]]]))

(defmethod interface/form-element :text-field [path _]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui default]} option]
      [text-field path
       :default default
       :label (:label ui)])))
