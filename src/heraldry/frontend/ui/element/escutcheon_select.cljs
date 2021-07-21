(ns heraldry.frontend.ui.element.escutcheon-select
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.ui.shared :as shared]
            [re-frame.core :as rf]))

(defn escutcheon-choice [path key display-name & {:keys [selected?]}]
  (let [{:keys [result]} (render/coat-of-arms
                          [:context :coat-of-arms]
                          100
                          (-> shared/coa-select-option-context
                              (assoc :coat-of-arms
                                     (if (= key :none)
                                       {:escutcheon :rectangle
                                        :field {:type :heraldry.field.type/plain
                                                :tincture :void}}
                                       {:escutcheon key
                                        :field {:type :heraldry.field.type/plain
                                                :tincture (if selected? :or :azure)}}))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set path key])}
     [:svg {:style {:width "4em"
                    :height "5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:transform "translate(10,10)"}
       result]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn escutcheon-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Escutcheon" (get escutcheon/choice-map value) {:width "17.5em"}
         (for [[display-name key] choices]
           ^{:key key}
           [escutcheon-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path]]])))

(defmethod interface/form-element :escutcheon-select [path]
  [escutcheon-select path])
