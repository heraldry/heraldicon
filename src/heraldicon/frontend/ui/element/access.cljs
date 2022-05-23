(ns heraldicon.frontend.ui.element.access
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn access [context & {:keys [disabled? on-change style option]}]
  (when-let [option (or option
                        (interface/get-relevant-options context))]
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
      [:div.ui-setting {:style style}
       [:input {:type "checkbox"
                :id component-id
                :checked (or checked? false)
                :disabled (or disabled? false)
                :on-change #(let [new-checked? (-> % .-target .-checked)]
                              (if on-change
                                (on-change new-checked?)
                                (rf/dispatch [:set context (if new-checked?
                                                             :public
                                                             :private)])))}]
       [:label.for-checkbox {:for component-id} [tr label]]])))

(defmethod ui.interface/form-element :access [context]
  [access context])
