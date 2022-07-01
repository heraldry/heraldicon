(ns heraldicon.frontend.element.checkbox
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn checkbox [context & {:keys [disabled? on-change style option]}]
  (when-let [option (or option
                        (interface/get-relevant-options context))]
    (let [component-id (uid/generate "checkbox")
          {:keys [ui inherited default]} option
          {:keys [label tooltip]} ui
          current-value (interface/get-raw-data context)
          checked? (->> [current-value
                         inherited
                         default]
                        (keep (fn [v]
                                (when-not (nil? v)
                                  v)))
                        first)]
      [:div.ui-setting {:style style}
       [:input {:type "checkbox"
                :id component-id
                :checked (or checked? false)
                :disabled (or disabled? false)
                :on-change #(let [new-checked? (-> % .-target .-checked)]
                              (if on-change
                                (on-change new-checked?)
                                (rf/dispatch [:set context new-checked?])))}]
       [:label.for-checkbox {:for component-id} [tr label]]
       [value-mode-select/value-mode-select context :disabled? disabled?]
       (when tooltip
         [:div.tooltip.info {:style {:display "inline-block"
                                     :margin-left "0.2em"}}
          [:i.fas.fa-question-circle]
          [:div.bottom
           [:h3 {:style {:text-align "center"}} [tr tooltip]]
           [:i]]])])))

(defmethod element/element :checkbox [context]
  [checkbox context])
