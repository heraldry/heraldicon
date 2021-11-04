(ns heraldry.frontend.ui.element.checkbox
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(defn checkbox [path & {:keys [disabled? on-change style option]}]
  (when-let [option (or option
                        @(rf/subscribe [:get-relevant-options path]))]
    (let [component-id (util/id "checkbox")
          {:keys [ui inherited default]} option
          label (:label ui)
          current-value @(rf/subscribe [:get path])
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
                                (rf/dispatch [:set path new-checked?])))}]
       [:label.for-checkbox {:for component-id} [tr label]]
       [value-mode-select/value-mode-select {:path path} :disabled? disabled?]])))

(defmethod ui-interface/form-element :checkbox [{:keys [path]}]
  [checkbox path])
