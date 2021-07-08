(ns heraldry.frontend.ui.element.select
  (:require [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn select [path & {:keys [on-change]}]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui default inherited choices]} option
          label (:label ui)
          component-id (util/id "select")
          value (or current-value
                    inherited
                    default
                    :none)]
      [:div.ui-setting
       (when label
         [:label {:for component-id} label])
       [:div.option
        [:select {:id component-id
                  :value (util/keyword->str value)
                  :on-change #(let [selected (keyword (-> % .-target .-value))]
                                (if on-change
                                  (on-change selected)
                                  (rf/dispatch [:set path selected])))}
         (for [[group-name & group-choices] choices]
           (if (and (-> group-choices count (= 1))
                    (-> group-choices first keyword?))
             (let [key (-> group-choices first)]
               ^{:key key}
               [:option {:value (util/keyword->str key)} group-name])
             ^{:key group-name}
             [:optgroup {:label group-name}
              (for [[display-name key] group-choices]
                ^{:key key}
                [:option {:value (util/keyword->str key)} display-name])]))]
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :select [path _]
  [select path])
