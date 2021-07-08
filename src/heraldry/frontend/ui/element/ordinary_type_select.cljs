(ns heraldry.frontend.ui.element.ordinary-type-select
  (:require [heraldry.coat-of-arms.ordinary.core :as ordinary]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.value-mode-select :as value-mode-select]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn ordinary-type-choice [path key display-name & {:keys [selected?]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :heraldry.field.type/plain
                                   :tincture :argent
                                   :components [{:type key
                                                 :line (when (= key :heraldry.ordinary.type/gore)
                                                         {:type :enarched
                                                          :flipped? true})
                                                 :origin (case key
                                                           :heraldry.ordinary.type/label {:alignment :left}
                                                           nil)
                                                 :geometry (case key
                                                             :heraldry.ordinary.type/label {:width 75
                                                                                            :size 12
                                                                                            :thickness 20}
                                                             :heraldry.ordinary.type/pile {:stretch 0.85}
                                                             nil)
                                                 :field {:type :heraldry.field.type/plain
                                                         :tincture (if selected? :or :azure)}}]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:set-ordinary-type (vec (drop-last path)) key])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(defn ordinary-type-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Ordinary" (get ordinary/ordinary-map value) {:width "21.5em"}
         (for [[display-name key] ordinary/choices]
           ^{:key key}
           [ordinary-type-choice path key display-name :selected? (= key value)])]
        [value-mode-select/value-mode-select path
         :display-fn ordinary/ordinary-map]]])))

(defmethod interface/form-element :ordinary-type-select [path _]
  [ordinary-type-select path])
