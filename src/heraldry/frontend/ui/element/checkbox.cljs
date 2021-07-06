(ns heraldry.frontend.ui.element.checkbox
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn checkbox [path & {:keys [default disabled? label on-change style]}]
  (let [component-id (util/id "checkbox")
        checked? (or @(rf/subscribe [:get-value path])
                     default)]
    [:div.ui-setting {:style style}
     [:input {:type "checkbox"
              :id component-id
              :checked checked?
              :disabled disabled?
              :on-change #(let [new-checked? (-> % .-target .-checked)]
                            (if on-change
                              (on-change new-checked?)
                              (rf/dispatch [:set path new-checked?])))}]
     [:label.for-checkbox {:for component-id} label]]))

(defmethod interface/form-element :checkbox [path {:keys [ui default] :as option}]
  (when option
    [checkbox path
     :default default
     :label (:label ui)]))
