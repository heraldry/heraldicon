(ns heraldicon.frontend.element.access
  (:require
   [heraldicon.frontend.interface :as ui.interface]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defmethod ui.interface/form-element :access [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [component-id (uid/generate "access")
          {:keys [ui default]} option
          label (:label ui)
          current-value (interface/get-raw-data context)
          checked? (->> [current-value
                         default]
                        (keep (fn [v]
                                (when-not (nil? v)
                                  v)))
                        first
                        (= :public))]
      [:div.ui-setting
       [:input {:type "checkbox"
                :id component-id
                :checked (or checked? false)
                :on-change #(let [new-checked? (-> % .-target .-checked)]
                              (rf/dispatch [:set context (if new-checked?
                                                           :public
                                                           :private)]))}]
       [:label.for-checkbox {:for component-id} [tr label]]])))
