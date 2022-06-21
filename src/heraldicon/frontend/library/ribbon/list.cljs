(ns heraldicon.frontend.library.ribbon.list
  (:require
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.ribbon.shared :refer [form-id]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.ui.element.ribbon-select :as ribbon-select]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn- on-select [{:keys [id]}]
  {:href (reife/href :route.ribbon.details/by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [::message/clear form-id]))})

(defn view []
  (rf/dispatch [::title/set :string.menu/ribbon-library])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p [tr :string.text.ribbon-library/create-and-view-ribbons]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [::message/clear form-id])
                  (reife/push-state :route.ribbon/create))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [ribbon-select/list-ribbons on-select]]])