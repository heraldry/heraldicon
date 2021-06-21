(ns heraldry.frontend.ui.element.escutcheon-select
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.form.state]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn escutcheon-choice [path key display-name & {:keys [selected?]}]
  (let [{:keys [result]} (render/coat-of-arms
                          (if (= key :none)
                            {:escutcheon :rectangle
                             :field {:type :heraldry.field.type/plain
                                     :tincture :void}}
                            {:escutcheon key
                             :field {:type :heraldry.field.type/plain
                                     :tincture (if selected? :or :azure)}})
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style {:width "4em"
                    :height "5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn escutcheon-select [path choices & {:keys [default label]}]
  (let [value (or @(rf/subscribe [:get-value path])
                  default)]
    [:div.ui-setting
     (when label
       [:label label])
     [:div.option
      [submenu/submenu path "Select Escutcheon" (get escutcheon/choice-map value) {:width "17.5em"}
       (for [[display-name key] choices]
         ^{:key key}
         [escutcheon-choice path key display-name :selected? (= key value)])]]]))

(defmethod interface/form-element :escutcheon-select [path {:keys [ui default choices] :as option}]
  (when option
    [escutcheon-select path choices
     :default default
     :label (:label ui)]))
