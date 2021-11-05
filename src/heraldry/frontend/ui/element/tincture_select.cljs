(ns heraldry.frontend.ui.element.tincture-select
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.static :as static]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(defn tincture-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                        :style {:border (if selected?
                                          "1px solid #000"
                                          "1px solid transparent")
                                :border-radius "5px"}}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url (str "/svg/tincture-" (name key) ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} [tr display-name]]
    [:i]]])

(defn tincture-select [{:keys [path] :as context} & {:keys [default-option]}]
  (when-let [option (or (interface/get-relevant-options context)
                        default-option)]
    (let [current-value (interface/get-raw-data context)
          {:keys [ui choices]} option
          value (options/get-value current-value option)
          tincture-map (util/choices->map choices)
          label (or (:label ui) strings/tincture)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context {:en "Select Tincture"
                                  :de "Tinktur auswählen"} (get tincture-map value) {:style {:width "22em"}}
         (doall
          (for [[group-name & group] choices]
            ^{:key group-name}
            [:<>
             [:h4 [tr group-name]]
             (doall
              (for [[display-name key] group]
                ^{:key display-name}
                [tincture-choice path key display-name :selected? (= key value)]))]))]
        [value-mode-select/value-mode-select context
         :default-option default-option]]])))

(defmethod ui-interface/form-element :tincture-select [context]
  [tincture-select context])
