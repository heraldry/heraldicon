(ns heraldicon.frontend.library.arms.list
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.arms.shared :refer [entity-type base-context]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.ui.element.arms-select :as arms-select]
   [heraldicon.frontend.ui.element.blazonry-editor.core :as blazonry-editor]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn on-select [{:keys [id]}]
  {:href (reife/href :route.arms.details/by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [::message/clear entity-type]))})

(defn view []
  (rf/dispatch [::title/set :string.entity/arms])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p [tr :string.text.arms-library/create-and-view-arms]]
    [:p [tr :string.text.arms-library/svg-png-access-info]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [::message/clear entity-type])
                  (reife/push-state :route.arms.details/create))}
    [tr :string.button/create]]
   " "
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [::message/clear entity-type])
                  (reife/push-state :route.arms.details/create)
                  (blazonry-editor/open (c/++ base-context :data :achievement :coat-of-arms :field)))}
    [tr :string.button/create-from-blazon]]
   [:div {:style {:padding-top "0.5em"}}
    [arms-select/list-arms on-select]]])
