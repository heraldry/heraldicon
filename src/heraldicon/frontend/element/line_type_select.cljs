(ns heraldicon.frontend.element.line-type-select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(defn- line-type-choice [context key display-name & {:keys [selected?
                                                            clickable?]
                                                     :or {clickable? true}}]
  (let [choice [:img.clickable {:style {:width "7.5em"}
                                :on-click (when clickable?
                                            (js-event/handled
                                             #(rf/dispatch [:set context key])))
                                :src (static/static-url
                                      (str "/svg/line-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defmethod element/element :ui.element/line-type-select [context]
  (when-let [option (interface/get-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [inherited default choices]
           :ui/keys [label]} option
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
           [line-type-choice context value choice-name :clickable? false]]]
         {:style {:width "24em"}}
         (into [:<>]
               (map (fn [[display-name key]]
                      ^{:key display-name}
                      [line-type-choice context key display-name :selected? (= key value)]))
               choices)]]])))
