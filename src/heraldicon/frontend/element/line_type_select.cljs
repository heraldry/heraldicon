(ns heraldicon.frontend.element.line-type-select
  (:require
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.interface :as ui.interface]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.static :as static]))

(defn- line-type-choice [context key display-name & {:keys [selected?
                                                            on-click?]
                                                     :or {on-click? true}}]
  [:div.choice.tooltip {:on-click (when on-click?
                                    #(state/dispatch-on-event % [:set context key]))}
   [:img.clickable {:style {:width "7.5em"}
                    :src (static/static-url
                          (str "/svg/line-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   (when on-click?
     [:div.bottom
      [:h3 {:style {:text-align "center"}} [tr display-name]]
      [:i]])])

(defmethod ui.interface/form-element :line-type-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [ui inherited default choices]} option
          label (:label ui)
          value (or current-value
                    inherited
                    default)
          choice-map (options/choices->map choices)
          choice-name (get choice-map value)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-line-type
         [:div
          [:div
           [tr choice-name]
           [value-mode-select/value-mode-select context]]
          [:div {:style {:transform "translate(-0.6em,0)"}}
           [line-type-choice context value choice-name :on-click? false]]]
         {:style {:width "24em"}}
         (into [:<>]
               (map (fn [[display-name key]]
                      ^{:key display-name}
                      [line-type-choice context key display-name :selected? (= key value)]))
               choices)]]])))
