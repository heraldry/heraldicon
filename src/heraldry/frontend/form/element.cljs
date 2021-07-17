(ns heraldry.frontend.form.element
  (:require [heraldry.util :refer [id]]
            [re-frame.core :as rf]))

(defn checkbox [path label & {:keys [on-change disabled? checked? style]}]
  (let [component-id (id "checkbox")
        checked? (-> (and path
                          @(rf/subscribe [:get path]))
                     (or checked?)
                     boolean
                     (and (not disabled?)))]
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

(defn radio-select [path choices & {:keys [on-change default label]}]
  [:div.setting
   (when label
     [:label label])
   [:div (when label
           {:class "other"
            :style {:display "inline-block"}})
    (let [current-value (or @(rf/subscribe [:get path])
                            default)]
      (for [[display-name key] choices]
        (let [component-id (id "radio")]
          ^{:key key}
          [:<>
           [:input {:id component-id
                    :type "radio"
                    :value (name key)
                    :checked (= key current-value)
                    :on-change #(let [value (keyword (-> % .-target .-value))]
                                  (if on-change
                                    (on-change value)
                                    (rf/dispatch [:set path value])))}]
           [:label {:for component-id
                    :style {:margin-right "10px"}} display-name]])))]])

(defn search-field [db-path & {:keys [on-change]}]
  (let [current-value @(rf/subscribe [:get db-path])
        input-id (id "input")]
    [:div {:style {:display "inline-block"
                   :border-radius "999px"
                   :border "1px solid #ccc"
                   :padding "3px 6px"
                   :min-width "10em"
                   :max-width "20em"
                   :width "50%"
                   :margin-bottom "0.5em"}}
     [:i.fas.fa-search]
     [:input {:id input-id
              :name "search"
              :type "text"
              :value current-value
              :autoComplete "off"
              :on-change #(let [value (-> % .-target .-value)]
                            (if on-change
                              (on-change value)
                              (rf/dispatch-sync [:set db-path value])))
              :style {:outline "none"
                      :border "0"
                      :margin-left "0.5em"
                      :width "calc(100% - 12px - 1.5em)"}}]]))
