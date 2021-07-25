(ns heraldry.frontend.ui.element.tincture-select
  (:require [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.options :as options]
            [re-frame.core :as rf]))

(defn tincture-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                        :style {:border (if selected?
                                          "1px solid #000"
                                          "1px solid transparent")
                                :border-radius "5px"}}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (str "/svg/tincture-" (name key) ".svg")}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} display-name]
    [:i]]])

(defn tincture-select [path & {:keys [default-option]}]
  (when-let [option (or @(rf/subscribe [:get-relevant-options path])
                        default-option)]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui choices]} option
          value (options/get-value current-value option)
          label (or (:label ui) "Tincture")]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Tincture" (get tincture/tincture-map value) {:width "22em"}
         (doall
          (for [[group-name & group] choices]
            ^{:key group-name}
            [:<>
             [:h4 group-name]
             (doall
              (for [[display-name key] group]
                ^{:key display-name}
                [tincture-choice path key display-name :selected? (= key value)]))]))]
        [value-mode-select/value-mode-select path
         :default-option default-option]]])))

(defmethod interface/form-element :tincture-select [path]
  [tincture-select path])
