(ns heraldry.frontend.ui.element.radio-select
  (:require [heraldry.frontend.language :refer [tr]]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
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
              :style {:margin-right "10px"}}
      [tr display-name]]]))

(defn radio-select [path & {:keys [on-change option]}]
  (when-let [option (or option
                        @(rf/subscribe [:get-relevant-options path]))]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          label (:label ui)
          value (or current-value
                    inherited
                    default)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        (for [[display-name key] choices]
          ^{:key key}
          [radio-choice path key display-name
           :selected? (= key value)
           :on-change on-change])
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :radio-select [path]
  [radio-select path])
