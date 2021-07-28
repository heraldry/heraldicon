(ns heraldry.frontend.ui.element.charge-type-select
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.frontend.charge :as frontend-charge]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.charge-select :as charge-select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.static :as static]
            [re-frame.core :as rf]))

(rf/reg-event-db :update-charge
  (fn [db [_ path changes]]
    (update-in db path merge changes)))

(defn charge-type-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:update-charge (vec (drop-last path)) {:type key
                                                                                                      :attitude nil
                                                                                                      :facing nil
                                                                                                      :data nil
                                                                                                      :variant nil}])}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url
                          (str "/svg/charge-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} display-name]
    [:i]]])

(defn charge-type-select [path]
  (when-let [option @(rf/subscribe [:get-relevant-options path])]
    (let [current-value @(rf/subscribe [:get-value path])
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)
          charge-path (vec (drop-last path))]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path "Select Charge" (charge/title charge-path {}) {:width "21.5em"}
         (for [[display-name key] choices]
           ^{:key key}
           [charge-type-choice path key display-name :selected? (= key value)])
         (let [[status charges] (state/async-fetch-data
                                 [:all-charges]
                                 :all-charges
                                 frontend-charge/fetch-charges)]
           [:div {:style {:padding "15px"}}
            (if (= status :done)
              [charge-select/component
               charges
               (fn [charge-data]
                 [:a.clickable
                  {:on-click #(state/dispatch-on-event
                               %
                               [:update-charge
                                (vec (drop-last path))
                                (merge {:type (->> charge-data
                                                   :type
                                                   name
                                                   (keyword "heraldry.charge.type"))
                                        :variant {:id (:id charge-data)
                                                  :version (:latest-version charge-data)}}
                                       {:attitude nil
                                        :facing nil}
                                       (select-keys charge-data
                                                    [:attitude :facing]))])}
                  (:name charge-data)])
               #(state/invalidate-cache [:all-charges] :all-charges)]
              [:div "loading..."])])]]])))

(defmethod interface/form-element :charge-type-select [path]
  [charge-type-select path])
