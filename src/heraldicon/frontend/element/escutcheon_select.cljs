(ns heraldicon.frontend.element.escutcheon-select
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

(defn- escutcheon-choice [context key display-name & {:keys [selected?
                                                             clickable?]
                                                      :or {clickable? true}}]
  (let [choice [:img.clickable {:style {:width "4em"
                                        :vertical-align "top"}
                                :on-click (when clickable?
                                            (js-event/handled #(rf/dispatch [:set context key])))
                                :src (static/static-url
                                      (str "/svg/escutcheon-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defmethod element/element :ui.element/escutcheon-select [context]
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
        [submenu/submenu context
         :string.escutcheon.type/select-escutcheon
         [:div
          [:div
           [tr choice-name]
           [value-mode-select/value-mode-select context]]
          [:div {:style {:transform "translate(-0.333em,0)"}}
           [escutcheon-choice context value choice-name :clickable? false]]]
         {:style {:width "26em"
                  :vertical-align "top"}}
         (into [:<>]
               (map (fn [[group-name & group]]
                      (into
                       ^{:key group-name}
                       [:<>
                        [:h4 [tr group-name]]]
                       (map (fn [[display-name key]]
                              ^{:key display-name}
                              [escutcheon-choice context key display-name :selected? (= key value)]))
                       group)))
               choices)]]])))
