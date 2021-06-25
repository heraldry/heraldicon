(ns heraldry.frontend.ui.element.field-type-select
  (:require [heraldry.coat-of-arms.field.core :as field]
            [heraldry.coat-of-arms.field.options :as field-options]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn field-type-choice [path key display-name & {:keys [selected?]}]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field (if (= key :heraldry.field.type/plain)
                                    {:type :heraldry.field.type/plain
                                     :tincture (if selected? :or :azure)}
                                    {:type key
                                     :fields (-> (field/default-fields {:type key})
                                                 (util/replace-recursively :none :argent)
                                                 (cond->
                                                  selected? (util/replace-recursively :azure :or)))
                                     :layout {:num-fields-x (case key
                                                              :heraldry.field.type/chequy 4
                                                              :heraldry.field.type/lozengy 3
                                                              :heraldry.field.type/vairy 2
                                                              :heraldry.field.type/potenty 2
                                                              :heraldry.field.type/papellony 2
                                                              :heraldry.field.type/masonry 2
                                                              nil)
                                              :num-fields-y (case key
                                                              :heraldry.field.type/chequy 5
                                                              :heraldry.field.type/lozengy 4
                                                              :heraldry.field.type/vairy 3
                                                              :heraldry.field.type/potenty 3
                                                              :heraldry.field.type/papellony 4
                                                              :heraldry.field.type/masonry 4
                                                              nil)}})}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(let [;; TODO: this should move into the event handler
                                           field-path (vec (drop-last path))
                                           field @(rf/subscribe [:get field-path])
                                           new-field (assoc field :type key)
                                           {:keys [num-fields-x
                                                   num-fields-y
                                                   num-base-fields]} (:layout (options/sanitize-or-nil
                                                                               new-field
                                                                               (field-options/options new-field)))]
                                       (state/dispatch-on-event % [:set-field-type field-path key num-fields-x num-fields-y num-base-fields]))}
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

(defn field-type-select [path choices & {:keys [default label]}]
  (let [value (or @(rf/subscribe [:get-value path])
                  default)]
    [:div.ui-setting
     (when label
       [:label label])
     [:div.option
      [submenu/submenu path "Select Division" (get field/field-map value) {:width "21.5em"}
       (for [[display-name key] field/choices]
         ^{:key key}
         [field-type-choice path key display-name :selected? (= key value)])]]]))

(defmethod interface/form-element :field-type-select [path {:keys [ui default choices] :as option}]
  (when option
    [field-type-select path choices
     :default default
     :label (:label ui)]))
