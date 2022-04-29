(ns heraldicon.frontend.ui.element.radio-select
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.util :as util]
   [re-frame.core :as rf]))

(defn radio-choice [context key display-name & {:keys [selected? on-change]}]
  (let [component-id (util/id "radio")]
    [:<>
     [:input {:id component-id
              :type "radio"
              :value (util/keyword->str key)
              :checked selected?
              :on-change #(let [value (keyword (-> % .-target .-value))]
                            (if on-change
                              (on-change value)
                              (rf/dispatch [:set context value])))}]
     [:label {:for component-id
              :style {:margin-right "10px"}}
      [tr display-name]]]))

(defn radio-select [context & {:keys [on-change option]}]
  (when-let [option (or option
                        (interface/get-relevant-options context))]
    (let [current-value (interface/get-raw-data context)
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
          [radio-choice context key display-name
           :selected? (= key value)
           :on-change on-change])
        [value-mode-select/value-mode-select context]]])))

(defmethod ui.interface/form-element :radio-select [context]
  [radio-select context])
