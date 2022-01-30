(ns heraldry.frontend.ui.element.charge-type-select
  (:require
   [heraldry.coat-of-arms.charge.options :as charge-options]
   [heraldry.context :as c]
   [heraldry.frontend.charge :as frontend-charge]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.preview :as preview]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.charge-select :as charge-select]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.static :as static]))

(macros/reg-event-db :update-charge
  (fn [db [_ path changes]]
    (update-in db path merge changes)))

(defn charge-type-choice [path key display-name & {:keys [selected?]}]
  [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:update-charge path {:type key
                                                                                    :attitude nil
                                                                                    :facing nil
                                                                                    :data nil
                                                                                    :variant nil}])}
   [:img.clickable {:style {:width "4em"
                            :height "4.5em"}
                    :src (static/static-url
                          (str "/svg/charge-type-" (name key) "-" (if selected? "selected" "unselected") ".svg"))}]
   [:div.bottom
    [:h3 {:style {:text-align "center"}} [tr display-name]]
    [:i]]])

(defn choice-preview-url [context]
  (let [type-context (c/++ context :type)
        variant-context (c/++ context :variant)
        {:keys [inherited default]} (interface/get-relevant-options type-context)
        current-value (interface/get-raw-data type-context)
        value (or current-value
                  inherited
                  default)
        variant (interface/get-raw-data variant-context)]
    (if variant
      (preview/preview-url
       :charge variant
       :width 64
       :height 72)
      (static/static-url
       (str "/svg/charge-type-" (name value) "-unselected.svg")))))

(defn choice-preview [context]
  (let [variant-context (c/++ context :variant)
        variant (interface/get-raw-data variant-context)
        img-url (choice-preview-url context)]
    [:div {:style {:transform (when (not variant)
                                "translate(-0.333em,0)")}}
     [:img.clickable {:src img-url
                      :style {:width "4em"
                              :height "4.5em"
                              :border (when variant
                                        "1.5px solid #ddd")}}]]))

(defn charge-type-select [context]
  (when-let [option (interface/get-relevant-options context)]
    (let [current-value (interface/get-raw-data context)
          {:keys [ui inherited default choices]} option
          value (or current-value
                    inherited
                    default)
          label (:label ui)
          charge-context (c/-- context)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-charge
         ;; TODO: this could have a proper preview of the charge
         [:div
          [tr (charge-options/title charge-context)]
          [choice-preview charge-context]]
         {:style {:width "21.5em"}}
         (for [[display-name key] choices]
           ^{:key key}
           [charge-type-choice (:path charge-context) key display-name :selected? (= key value)])
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
                                (:path charge-context)
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
              [:div [tr :string.miscellaneous/loading]])])]]])))

(defmethod ui-interface/form-element :charge-type-select [context]
  [charge-type-select context])
