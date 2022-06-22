(ns heraldicon.frontend.library.user
  (:require
   [heraldicon.avatar :as avatar]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.arms.list :as library.arms.list]
   [heraldicon.frontend.library.charge.list :as library.charge.list]
   [heraldicon.frontend.library.collection.list :as library.collection.list]
   [heraldicon.frontend.loading :as loading]
   [heraldicon.frontend.repository.entity-list-for-user :as entity-list-for-user]
   [heraldicon.frontend.repository.user :as repository.user]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.ui.element.arms-select :as arms-select]
   [heraldicon.frontend.ui.element.charge-select :as charge-select]
   [heraldicon.frontend.ui.element.collection-select :as collection-select]
   [heraldicon.frontend.ui.element.user-select :as user-select]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn- view-charges-for-user [user-id]
  [charge-select/component
   (rf/subscribe [::entity-list-for-user/data :heraldicon.entity.type/charge user-id])
   library.charge.list/on-select
   #(rf/dispatch [::entity-list-for-user/clear :heraldicon.entity.type/charge user-id])
   :remove-empty-groups? true
   :hide-ownership-filter? true])

(defn- view-arms-for-user [user-id]
  [arms-select/component
   (rf/subscribe [::entity-list-for-user/data :heraldicon.entity.type/arms user-id])
   library.arms.list/on-select
   #(rf/dispatch [::entity-list-for-user/clear :heraldicon.entity.type/arms user-id])
   :hide-ownership-filter? true])

(defn- view-collections-for-user [user-id]
  (let [{:keys [status entities]} @(rf/subscribe [::entity-list-for-user/data :heraldicon.entity.type/collection user-id])]
    (if (= status :done)
      [collection-select/component
       entities
       library.collection.list/link-to-collection
       #(rf/dispatch [::entity-list-for-user/clear :heraldicon.entity.type/collection user-id])
       :hide-ownership-filter? true]
      [loading/loading])))

(defn- user-display [username]
  (let [{status :status
         user-info-data :user} @(rf/subscribe [::repository.user/data username])
        user-id (:id user-info-data)]
    (if (= status :done)
      (do
        (rf/dispatch [::title/set (:username user-info-data)])
        [:div {:style {:display "grid"
                       :grid-gap "10px"
                       :grid-template-columns "[start] minmax(10em, 20%) [first] minmax(10em, 40%) [second] minmax(10em, 40%) [end]"
                       :grid-template-rows "[top] 100px [middle] auto [bottom]"
                       :grid-template-areas "'user-info user-info user-info'
                                       'collections arms charges'"
                       :padding-left "20px"
                       :padding-right "10px"
                       :height "100%"}}
         [:div.no-scrollbar {:style {:grid-area "user-info"
                                     :overflow-y "scroll"
                                     :padding-top "10px"}}
          [:img {:src (avatar/url (:username user-info-data))
                 :style {:width "80px"
                         :height "80px"}}]
          [:h2 {:style {:display "inline-block"
                        :vertical-align "top"
                        :margin-left "1em"}}
           (:username user-info-data)]]
         [:div.no-scrollbar {:style {:grid-area "collections"
                                     :overflow-y "scroll"}}
          [:h4 [tr :string.entity/collections]]
          [view-collections-for-user user-id]]
         [:div.no-scrollbar {:style {:grid-area "arms"
                                     :overflow-y "scroll"}}
          [:h4 [tr :string.entity/arms]]
          [view-arms-for-user user-id]]
         [:div.no-scrollbar {:style {:grid-area "charges"
                                     :overflow-y "scroll"}}
          [:h4 [tr :string.entity/charges]]
          [view-charges-for-user user-id]]])
      [loading/loading])))

(defn details-view [{{{:keys [username]} :path} :parameters}]
  [user-display username])

(defn- link-to-user [{:keys [username]}]
  [:a {:href (reife/href :route.user/details {:username username})}
   username])

(defn list-view []
  (rf/dispatch [::title/set :string.menu/users])
  [:div {:style {:padding "15px"}}
   (when (entity.user/admin? (user/data))
     [user-select/list-users link-to-user])])
