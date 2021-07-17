(ns heraldry.frontend.ui.element.line-type-select
  (:require [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.ui.shared :as shared]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn line-type-choice [path key display-name & {:keys [selected?]}]
  (let [options (line/options {:type key})
        {:keys [result]} (render/coat-of-arms
                          {:escutcheon :flag
                           :field {:type :heraldry.field.type/per-fess
                                   :line {:type key
                                          :width (case key
                                                   :enarched nil
                                                   (* 2 (options/get-value nil (:width options))))
                                          :height (case key
                                                    :enarched 0.25
                                                    nil)}
                                   :fields [{:type :heraldry.field.type/plain
                                             :tincture :argent}
                                            {:type :heraldry.field.type/plain
                                             :tincture (if selected? :or :azure)}]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style {:width "6.5em"
                    :height "4.5em"}
            :viewBox "0 0 120 80"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn line-type-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          label (:label ui)
          value (or current-value
                    inherited
                    default)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Line Type" (get line/line-map value) {:min-width "25em"}
         (for [[display-name key] choices]
           ^{:key display-name}
           [line-type-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :line-type-select [path]
  [line-type-select path])
