(ns heraldicon.frontend.element.radio-select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.util.core :as util]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn- radio-choice [context key display-name & {:keys [selected? on-change]}]
  (let [component-id (uid/generate "radio")]
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
        (into [:<>]
              (map (fn [[display-name key]]
                     ^{:key key}
                     [radio-choice context key display-name
                      :selected? (= key value)
                      :on-change on-change]))
              choices)
        [value-mode-select/value-mode-select context]]])))

(defmethod element/element :ui.element/radio-select [context]
  [radio-select context])
