(ns heraldry.frontend.ui.element.escutcheon-select
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.static :as static]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(defn escutcheon-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
   [:img.clickable {:style {:width "4em"
                            :vertical-align "top"}
                    :src (static/static-url
                          (str "/svg/escutcheon-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} [tr display-name]]
    [:i]]])

(defn escutcheon-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get path])
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)
          choice-map (util/choices->map choices)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu path {:en "Select Escutcheon"
                               :de "Schild auswählen"} (get choice-map value) {:style {:width "17.5em"
                                                                                       :vertical-align "top"}}
         (for [[display-name key] choices]
           ^{:key key}
           [escutcheon-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select {:path path}]]])))

(defmethod ui-interface/form-element :escutcheon-select [{:keys [path]}]
  [escutcheon-select path])
