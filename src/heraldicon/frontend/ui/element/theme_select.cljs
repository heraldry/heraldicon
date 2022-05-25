(ns heraldicon.frontend.ui.element.theme-select
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.static :as static]))

(defn theme-choice [context key display-name & {:keys [selected?
                                                       on-click?]
                                                :or {on-click? true}}]
  [:div.choice.tooltip {:on-click (when on-click?
                                    #(state/dispatch-on-event % [:set context key]))
                        :style {:border (if selected?
                                          "1px solid #000"
                                          "1px solid transparent")
                                :border-radius "5px"}}
   [:img.clickable {:style {:width "4em"
                            :height (when-not (= key :all) "4.5em")}
                    :src (static/static-url (if (= key :all)
                                              "/img/psychedelic.png"
                                              (str "/svg/theme-" (name key) ".svg")))}]
   (when on-click?
     [:div.bottom
      [:h3 {:style {:text-align "center"}}
       [tr display-name]]
      [:i]])])

(defn theme-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [{:keys [ui inherited default choices]} option
          current-value (interface/get-raw-data context)
          value (or current-value
                    inherited
                    default)
          label (:label ui)
          choice-map (options/choices->map choices)
          choice-name (get choice-map value)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-colour-theme
         [:div
          [:div
           [tr choice-name]
           [value-mode-select/value-mode-select context]]
          [:div {:style {:transform "translate(-0.4em,0)"}}
           [theme-choice context value choice-name :on-click? false]]]
         {:style {:width "22em"}}
         (into [:<>]
               (map (fn [[group-name & group]]
                      (into
                       ^{:key group-name}
                       [:<>
                        [:h4 [tr group-name]]]
                       (map (fn [[display-name key]]
                              ^{:key display-name}
                              [theme-choice context key display-name :selected? (= key value)]))
                       group)))
               choices)]]])))

(defmethod ui.interface/form-element :theme-select [context]
  [theme-select context])
