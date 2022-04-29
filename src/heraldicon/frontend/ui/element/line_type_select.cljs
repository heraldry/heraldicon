(ns heraldicon.frontend.ui.element.line-type-select
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.static :as static]
   [heraldicon.util :as util]))

(defn line-type-choice [context key display-name & {:keys [selected?
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

(defn line-type-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [ui inherited default choices]} option
          label (:label ui)
          value (or current-value
                    inherited
                    default)
          choice-map (util/choices->map choices)
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
         (for [[display-name key] choices]
           ^{:key display-name}
           [line-type-choice context key display-name :selected? (= key value)])]]])))

(defmethod ui.interface/form-element :line-type-select [context]
  [line-type-select context])