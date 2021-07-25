(ns heraldry.frontend.ui.element.line-type-select
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn line-type-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
   [:img.clickable {:style {:width "7.5em"}
                    :src (str "/svg/line-" (name key) "-" (if selected? "selected" "unselected") ".svg")}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} display-name]
    [:i]]])

(defn line-type-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          label (:label ui)
          value (or current-value
                    inherited
                    default)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Line Type" (get line/line-map value) {:width "24em"}
         (for [[display-name key] choices]
           ^{:key display-name}
           [line-type-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :line-type-select [path]
  [line-type-select path])
