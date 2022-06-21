(ns heraldicon.frontend.library.collection.list
  (:require
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.collection.shared :refer [form-id]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.ui.element.collection-select :as collection-select]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn link-to-collection [collection]
  (let [collection-id (-> collection
                          :id
                          id/for-url)]
    [:a {:href (reife/href :route.collection.details/by-id {:id collection-id})
         :on-click #(do
                      (rf/dispatch-sync [::message/clear form-id]))}
     (:name collection)]))

(defn view []
  (rf/dispatch [::title/set :string.entity/collections])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p [tr :string.text.collection-library/create-and-view-collections]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [::message/clear form-id])
                  (reife/push-state :route.collection/create))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [collection-select/list-collections link-to-collection]]])