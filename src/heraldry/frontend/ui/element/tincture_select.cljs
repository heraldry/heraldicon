(ns heraldry.frontend.ui.element.tincture-select
  (:require [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn tincture-choice [path key display-name & {:keys [selected?]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :heraldry.field.type/plain
                                   :tincture key}}
                          40
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                          :style {:border (if selected?
                                            "1px solid #000"
                                            "1px solid transparent")
                                  :border-radius "5px"}}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 50 100"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(5,5)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn tincture-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (or (:label ui) "Tincture")]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Tincture" (get tincture/tincture-map value) {:width "22em"}
         (for [[group-name & group] choices]
           ^{:key group-name}
           [:<>
            [:h4 group-name]
            (for [[display-name key] group]
              ^{:key display-name}
              [tincture-choice path key display-name :selected? (= key value)])])]
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :tincture-select [path _]
  [tincture-select path])
