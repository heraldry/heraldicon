(ns heraldry.frontend.ui.element.theme-select
  (:require [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.static :as static]
            [re-frame.core :as rf]))

(defn theme-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                        :style {:border (if selected?
                                          "1px solid #000"
                                          "1px solid transparent")
                                :border-radius "5px"}}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url (str "/svg/theme-" (name key) ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} display-name]
    [:i]]])

(defn theme-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui inherited default choices]} option
          current-value @(rf/subscribe [:get-value path])
          value (or current-value
                    inherited
                    default)
          label (:label ui)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Colour Theme" (get tincture/theme-map value) {:width "22em"}
         (for [[group-name & group] choices]
           ^{:key group-name}
           [:<>
            [:h4 group-name]
            (for [[display-name key] group]
              ^{:key display-name}
              [theme-choice path key display-name :selected? (= key value)])])]
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :theme-select [path]
  [theme-select path])
