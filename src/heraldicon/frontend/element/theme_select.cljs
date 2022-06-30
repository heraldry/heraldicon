(ns heraldicon.frontend.element.theme-select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(defn- theme-choice [context key display-name & {:keys [selected?
                                                        on-click?]
                                                 :or {on-click? true}}]
  [:div.choice.tooltip {:on-click (when on-click?
                                    (js-event/handled #(rf/dispatch [:set context key])))
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

(defmethod element/element :theme-select [context]
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
