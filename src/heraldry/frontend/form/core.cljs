(ns heraldry.frontend.form.core
  (:require [heraldry.util :as util]
            [re-frame.core :as rf]))

(defn field [db-path function]
  (let [value @(rf/subscribe [:get db-path])
        error @(rf/subscribe [:get-form-error db-path])]
    [:div {:class (when error "error")}
     (when error
       [:div.error-message error])
     (function :value value
               :on-change #(let [new-value (-> % .-target .-value)]
                             (rf/dispatch [:set db-path new-value])))]))

(defn field-without-error [db-path function]
  (let [value @(rf/subscribe [:get db-path])]
    (function :value value
              :on-change #(let [new-value (-> % .-target .-value)]
                            (rf/dispatch [:set db-path new-value])))))

(defn checkbox [path label & {:keys [style]}]
  (let [component-id (util/id "checkbox")
        checked? (-> (and path
                          @(rf/subscribe [:get path]))
                     boolean)]
    [:label {:for component-id
             :style (merge {:text-align "left"
                            :width "6em"}
                           style)}
     [:input {:type "checkbox"
              :id component-id
              :checked checked?
              :on-change #(let [new-checked? (-> % .-target .-checked)]
                            (rf/dispatch [:set path new-checked?]))
              :style {:vertical-align "-1px"}}]
     (str " " label)]))

(defn select [path label choices & {:keys [grouped? value on-change default label-extra style label-style]}]
  (let [component-id (util/id "select")
        current-value @(rf/subscribe [:get path])]
    [:div.pure-control-group {:style style}
     [:label {:for component-id
              :style label-style} label label-extra]
     [:select {:id component-id
               :value (name (or value
                                current-value
                                default
                                :none))
               :on-change #(let [checked (keyword (-> % .-target .-value))]
                             (if on-change
                               (on-change checked)
                               (rf/dispatch [:set path checked])))}
      (if grouped?
        (for [[group-name & group-choices] choices]
          (if (and (-> group-choices count (= 1))
                   (-> group-choices first keyword?))
            (let [key (-> group-choices first)]
              ^{:key key}
              [:option {:value (name key)} group-name])
            ^{:key group-name}
            [:optgroup {:label group-name}
             (for [[display-name key] group-choices]
               ^{:key key}
               [:option {:value (name key)} display-name])]))
        (for [[display-name key] choices]
          ^{:key key}
          [:option {:value (name key)} display-name]))]]))
