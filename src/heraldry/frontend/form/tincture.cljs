(ns heraldry.frontend.form.tincture
  (:require [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(defn tincture-choice [path key display-name]
  (let [value @(rf/subscribe [:get path])
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :plain
                                   :tincture key}}
                          40
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                          :style {:border (if (= value key)
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

(defn form [path & {:keys [label] :or {label "Tincture"}}]
  (let [value (or @(rf/subscribe [:get path])
                  :none)
        names (-> tincture/tincture-map
                  (assoc :none "None"))]
    [:div.setting
     [:label label]
     " "
     [element/submenu path "Select Tincture" (get names value) {:min-width "22em"}
      (for [[group-name & group] tincture/choices]
        ^{:key group-name}
        [:<>
         (if (= group-name "Metal")
           [:<>
            [:h4 {:style {:margin-left "4.5em"}} group-name]
            [tincture-choice path :none "None"]]
           [:h4 group-name])
         (for [[display-name key] group]
           ^{:key display-name}
           [tincture-choice path key display-name])])]]))
