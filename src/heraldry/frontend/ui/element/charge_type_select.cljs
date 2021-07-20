(ns heraldry.frontend.ui.element.charge-type-select
  (:require [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.charge :as frontend-charge]
            [heraldry.frontend.form.charge-select :as charge-select]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.ui.shared :as shared]
            [heraldry.util :refer [full-url-for-username]]
            [re-frame.core :as rf]))

(rf/reg-event-db :update-charge
  (fn [db [_ path changes]]
    (update-in db path merge changes)))

(defn charge-type-choice [path key display-name & {:keys [selected?]}]
  (let [{:keys [result]} (render/coat-of-arms
                          [:coat-of-arms]
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:data :coat-of-arms]
                                        {:escutcheon :rectangle
                                         :field {:type :heraldry.field.type/plain
                                                 :tincture :argent
                                                 :components [{:type key
                                                               :geometry {:size 75}
                                                               :escutcheon (if (= key :heraldry.charge.type/escutcheon) :heater nil)
                                                               :field {:type :heraldry.field.type/plain
                                                                       :tincture (if selected? :or :azure)}}]}})))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:update-charge (vec (drop-last path)) {:type key
                                                                                                        :attitude nil
                                                                                                        :facing nil
                                                                                                        :data nil
                                                                                                        :variant nil}])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:transform "translate(10,10)"}
       result]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

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
        [submenu/submenu path "Select Charge" (charge/title charge-path) {:width "21.5em"}
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
               nil
               #(state/invalidate-cache [:all-charges] :all-charges)
               :render-variant (fn [node]
                                 (let [charge-data (:data node)
                                       username (:username charge-data)]
                                   [:div {:style {:display "inline-block"
                                                  :white-space "normal"
                                                  :vertical-align "top"
                                                  :line-height "1.5em"}}
                                    [:div {:style {:display "inline-block"
                                                   :vertical-align "top"}}
                                     (if (-> charge-data :is-public)
                                       [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
                                       [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]])
                                     " "
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
                                      (:name charge-data)]
                                     " by "
                                     [:a {:href (full-url-for-username username)
                                          :target "_blank"} username]]
                                    [charge-select/charge-properties charge-data]]))]
              [:div "loading..."])])]]])))

(defmethod interface/form-element :charge-type-select [path]
  [charge-type-select path])
