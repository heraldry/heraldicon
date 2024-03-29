(ns heraldicon.frontend.element.checkbox
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn checkbox [context & {:keys [disabled? on-change style option]}]
  (when-let [option (or option
                        (interface/get-options context))]
    (let [component-id (uid/generate "checkbox")
          {:keys [inherited default override]
           :ui/keys [label tooltip]} option
          disabled? (or (:ui/disabled? option) disabled?)
          current-value (if (some? override)
                          override
                          (interface/get-raw-data context))
          checked? (->> [current-value
                         inherited
                         default]
                        (keep (fn [v]
                                (when-not (nil? v)
                                  v)))
                        first)]
      [:div.ui-setting {:style (assoc style :white-space "nowrap")}
       [:input {:type "checkbox"
                :id component-id
                :checked (or checked? false)
                :disabled (or disabled? false)
                :on-change #(let [new-checked? (-> % .-target .-checked)]
                              (if on-change
                                (on-change new-checked?)
                                (rf/dispatch [:set context new-checked?])))}]
       [:label.for-checkbox {:for component-id
                             :style {:white-space "normal"}} [tr label]]
       [tooltip/info tooltip]
       [value-mode-select/value-mode-select context :disabled? disabled?]])))

(defmethod element/element :ui.element/checkbox [context]
  [checkbox context])
