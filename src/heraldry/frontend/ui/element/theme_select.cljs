(ns heraldry.frontend.ui.element.theme-select
  (:require [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn theme-choice [path key display-name & {:keys [selected?]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :heraldry.field.type/bendy-sinister
                                   :line {:type :straight}
                                   :layout {:num-base-fields 7
                                            :num-fields-y 7}
                                   :fields [{:type :heraldry.field.type/plain
                                             :tincture :argent}
                                            {:type :heraldry.field.type/plain
                                             :tincture :gules}
                                            {:type :heraldry.field.type/plain
                                             :tincture :or}
                                            {:type :heraldry.field.type/plain
                                             :tincture :vert}
                                            {:type :heraldry.field.type/plain
                                             :tincture :azure}
                                            {:type :heraldry.field.type/plain
                                             :tincture :purpure}
                                            {:type :heraldry.field.type/plain
                                             :tincture :sable}
                                            {:type :heraldry.field.type/ref
                                             :index 0}]}}
                          80
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] key)))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])
                          :style {:border (if selected?
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

(defn theme-select [path choices & {:keys [label default]}]
  (let [value (or @(rf/subscribe [:get-value path])
                  default)]
    [:div.ui-setting
     (when label
       [:label label])
     [:div.option
      [submenu/submenu path "Select Colour Theme" (get tincture/theme-map value) {:min-width "22em"}
       (for [[group-name & group] choices]
         ^{:key group-name}
         [:<>
          [:h4 group-name]
          (for [[display-name key] group]
            ^{:key display-name}
            [theme-choice path key display-name :selected? (= key value)])])]]]))

(defmethod interface/form-element :theme-select [path {:keys [ui default choices] :as option}]
  (when option
    [theme-select path choices
     :default default
     :label (:label ui)]))
