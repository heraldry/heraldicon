(ns heraldicon.frontend.library.collection.list
  (:require
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.element.collection-select :as collection-select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.collection.shared :refer [entity-type]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.title :as title]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn on-select [{:keys [id]}]
  {:href (reife/href :route.collection.details/by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [::message/clear entity-type]))})

(defn view []
  (rf/dispatch [::title/set :string.entity/collections])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p [tr :string.text.collection-library/create-and-view-collections]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [::message/clear entity-type])
                  (reife/push-state :route.collection.details/create))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [collection-select/list-collections on-select]]])
