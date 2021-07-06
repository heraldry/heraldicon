(ns heraldry.frontend.ui.element.radio-select
  (:require [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn radio-choice [path key display-name & {:keys [selected? on-change]}]
  (let [component-id (util/id "radio")]
    [:<>
     [:input {:id component-id
              :type "radio"
              :value (util/keyword->str key)
              :checked selected?
              :on-change #(let [value (keyword (-> % .-target .-value))]
                            (if on-change
                              (on-change value)
                              (rf/dispatch [:set path value])))}]
     [:label {:for component-id
              :style {:margin-right "10px"}} display-name]]))

(defn radio-select [path choices & {:keys [default label on-change]}]
  [:div.ui-setting
   (when label
     [:label label])
   [:div.option
    (let [current-value (or @(rf/subscribe [:get-value path])
                            default)]
      (for [[display-name key] choices]
        ^{:key key}
        [radio-choice path key display-name
         :selected? (= key current-value)
         :on-change on-change]))]])

(defmethod interface/form-element :radio-select [path _]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui default choices]} option]
      [radio-select path choices
       :default default
       :label (:label ui)])))
