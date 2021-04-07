(ns heraldry.frontend.form.theme
  (:require [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(defn theme-choice [path key display-name]
  (let [value @(rf/subscribe [:get path])
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :bendy-sinister
                                   :line {:type :straight}
                                   :layout {:num-base-fields 7
                                            :num-fields-y 7}
                                   :fields [{:type :plain
                                             :tincture :argent}
                                            {:type :plain
                                             :tincture :gules}
                                            {:type :plain
                                             :tincture :or}
                                            {:type :plain
                                             :tincture :vert}
                                            {:type :plain
                                             :tincture :azure}
                                            {:type :plain
                                             :tincture :purpure}
                                            {:type :plain
                                             :tincture :sable}
                                            {:ref 0}]}}
                          80
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] key)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                          :style {:border (if (= value key)
                                            "1px solid #000"
                                            "1px solid transparent")
                                  :border-radius "5px"}}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 100 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn form [path & {:keys [label] :or {label "Colour Theme"}}]
  (let [value (or @(rf/subscribe [:get path])
                  tincture/default-theme)]
    [:div.setting
     [:label label]
     " "
     [element/submenu path "Select Colour Theme" (get tincture/theme-map value) {:min-width "22em"}
      (for [[group-name & group] tincture/theme-choices]
        ^{:key group-name}
        [:<>
         [:h4 group-name]
         (for [[display-name key] group]
           ^{:key display-name}
           [theme-choice path key display-name])])]]))
