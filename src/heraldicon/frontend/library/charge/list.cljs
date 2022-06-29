(ns heraldicon.frontend.library.charge.list
  (:require
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.element.charge-select :as charge-select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.charge.shared :refer [entity-type]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.title :as title]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn on-select [{:keys [id]}]
  {:href (reife/href :route.charge.details/by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [::message/clear entity-type]))})

(defn view []
  (rf/dispatch [::title/set :string.entity/charges])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p [tr :string.text.charge-library/create-and-view-charges]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [::message/clear entity-type])
                  (reife/push-state :route.charge.details/create))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [charge-select/list-charges on-select]]])
