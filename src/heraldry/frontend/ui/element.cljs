(ns heraldry.frontend.ui.element
  (:require [heraldry.frontend.state :as state]
            [heraldry.util :refer [id]]
            [re-frame.core :as rf]))

(defn keyword->str [k]
  (-> k
      str
      (subs 1)))

(defn select [path choices & {:keys [default label on-change]}]
  (let [component-id (id "select")
        current-value @(rf/subscribe [:get-value path])]
    [:div.ui-setting
     (when label
       [:label {:for component-id} label])
     [:div.option
      [:select {:id component-id
                :value (keyword->str (or current-value
                                         default
                                         :none))
                :on-change #(let [selected (keyword (-> % .-target .-value))]
                              (if on-change
                                (on-change selected)
                                (rf/dispatch [:set path selected])))}
       (for [[group-name & group-choices] choices]
         (if (and (-> group-choices count (= 1))
                  (-> group-choices first keyword?))
           (let [key (-> group-choices first)]
             ^{:key key}
             [:option {:value (keyword->str key)} group-name])
           ^{:key group-name}
           [:optgroup {:label group-name}
            (for [[display-name key] group-choices]
              ^{:key key}
              [:option {:value (keyword->str key)} display-name])]))]]]))

(defn radio-select [path choices & {:keys [default label on-change]}]
  [:div.ui-setting
   (when label
     [:label label])
   [:div.option
    (let [current-value (or @(rf/subscribe [:get-value path])
                            default)]
      (for [[display-name key] choices]
        (let [component-id (id "radio")]
          ^{:key key}
          [:<>
           [:input {:id component-id
                    :type "radio"
                    :value (keyword->str key)
                    :checked (= key current-value)
                    :on-change #(let [value (keyword (-> % .-target .-value))]
                                  (if on-change
                                    (on-change value)
                                    (rf/dispatch [:set path value])))}]
           [:label {:for component-id
                    :style {:margin-right "10px"}} display-name]])))]])

(defn checkbox [path & {:keys [default disabled? label on-change style]}]
  (let [component-id (id "checkbox")
        checked? (or @(rf/subscribe [:get-value path])
                     default)]
    [:div.setting {:style style}
     [:input {:type "checkbox"
              :id component-id
              :checked checked?
              :disabled disabled?
              :on-change #(let [new-checked? (-> % .-target .-checked)]
                            (if on-change
                              (on-change new-checked?)
                              (rf/dispatch [:set path new-checked?])))}]
     [:label {:for component-id} label]]))

(defn submenu [path title link-name styles & content]
  (let [submenu-id (conj path title)
        submenu-open? @(rf/subscribe [:ui-submenu-open? submenu-id])]
    [:div.ui-submenu-setting {:style {:display "inline-block"}
                              :on-click #(.stopPropagation %)}
     [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-open submenu-id])}
      link-name]
     (when submenu-open?
       [:div.ui-component.ui-submenu {:style styles}
        [:div.header [:a {:on-click #(state/dispatch-on-event % [:ui-submenu-close submenu-id])}
                      [:i.far.fa-times-circle]]
         " " title]
        (into [:div.content]
              content)])]))
