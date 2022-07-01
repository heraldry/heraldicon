(ns heraldicon.frontend.element.tincture-select
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

(defn- tincture-choice [context key display-name & {:keys [selected?
                                                           clickable?]
                                                    :or {clickable? true}}]
  (let [choice [:div {:style {:border (if selected?
                                        "1px solid #000"
                                        "1px solid transparent")
                              :border-radius "5px"}}
                [:img.clickable {:style {:width "4em"
                                         :height "4.5em"}
                                 :on-click (when clickable?
                                             (js-event/handled #(rf/dispatch [:set context key])))
                                 :src (static/static-url (str "/svg/tincture-" (name key) ".svg"))}]]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defn tincture-select [context & {:keys [default-option]}]
  (when-let [option (or (interface/get-relevant-options context)
                        default-option)]
    (let [current-value (interface/get-raw-data context)
          {:keys [choices]
           :ui/keys [label]} option
          value (options/get-value current-value option)
          choice-map (options/choices->map choices)
          choice-name (get choice-map value)
          label (or label :string.option/tincture)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-tincture
         [:div
          [:div
           [tr choice-name]
           [value-mode-select/value-mode-select context :default-option default-option]]
          [:div {:style {:transform "translate(-0.4em,0)"}}
           [tincture-choice context value choice-name :clickable? false]]]
         {:style {:width "22em"}}
         (into [:<>]
               (map (fn [[group-name & group]]
                      (into
                       ^{:key group-name}
                       [:<>
                        [:h4 [tr group-name]]]
                       (map (fn [[display-name key]]
                              ^{:key display-name}
                              [tincture-choice context key display-name :selected? (= key value)]))
                       group)))
               choices)]]])))

(defmethod element/element :ui.element/tincture-select [context]
  [tincture-select context])
