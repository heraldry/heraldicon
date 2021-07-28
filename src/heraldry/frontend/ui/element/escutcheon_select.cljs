(ns heraldry.frontend.ui.element.escutcheon-select
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.static :as static]
            [re-frame.core :as rf]))

(defn escutcheon-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
   [:img.clickable {:style {:width "4em"
                            :height "5em"}
                    :src (static/static-url
                          (str "/svg/escutcheon-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} display-name]
    [:i]]])

(defn escutcheon-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Escutcheon" (get escutcheon/choice-map value) {:width "17.5em"}
         (for [[display-name key] choices]
           ^{:key key}
           [escutcheon-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :escutcheon-select [path]
  [escutcheon-select path])
