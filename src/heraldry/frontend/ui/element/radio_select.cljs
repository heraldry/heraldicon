(ns heraldry.frontend.ui.element.radio-select
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn radio-select [path choices & {:keys [default label on-change]}]
  [:div.ui-setting
   (when label
     [:label label])
   [:div.option
    (let [current-value (or @(rf/subscribe [:get-value path])
                            default)]
      (for [[display-name key] choices]
        (let [component-id (util/id "radio")]
          ^{:key key}
          [:<>
           [:input {:id component-id
                    :type "radio"
                    :value (util/keyword->str key)
                    :checked (= key current-value)
                    :on-change #(let [value (keyword (-> % .-target .-value))]
                                  (if on-change
                                    (on-change value)
                                    (rf/dispatch [:set path value])))}]
           [:label {:for component-id
                    :style {:margin-right "10px"}} display-name]])))]])

(defmethod interface/form-element :radio-select [path {:keys [ui default choices] :as option}]
  (when option
    [radio-select path choices
     :default default
     :label (:label ui)]))
