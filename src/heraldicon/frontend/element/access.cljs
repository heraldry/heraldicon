(ns heraldicon.frontend.element.access
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defmethod element/element :ui.element/access [context]
  (when-let [option (interface/get-options context)]
    (let [component-id (uid/generate "access")
          {:keys [default]
           :ui/keys [label]} option
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
