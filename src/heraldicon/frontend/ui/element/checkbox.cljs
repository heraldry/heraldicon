(ns heraldicon.frontend.ui.element.checkbox
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.util :as util]
   [re-frame.core :as rf]))

(defn checkbox [context & {:keys [disabled? on-change style option]}]
  (when-let [option (or option
                        (interface/get-relevant-options context))]
    (let [component-id (util/id "checkbox")
          {:keys [ui inherited default]} option
          label (:label ui)
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
       [value-mode-select/value-mode-select context :disabled? disabled?]])))

(defmethod ui.interface/form-element :checkbox [context]
  [checkbox context])