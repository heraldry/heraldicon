(ns heraldicon.frontend.library.user
  (:require
   [heraldicon.avatar :as avatar]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.element.arms-select :as arms-select]
   [heraldicon.frontend.element.charge-select :as charge-select]
   [heraldicon.frontend.element.collection-select :as collection-select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.arms.list :as library.arms.list]
   [heraldicon.frontend.library.charge.list :as library.charge.list]
   [heraldicon.frontend.library.collection.list :as library.collection.list]
   [heraldicon.frontend.repository.user :as repository.user]
   [heraldicon.frontend.repository.user-list :as repository.user-list]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(defn- view-charges-for-user [username]
  [charge-select/component
   library.charge.list/on-select
   {:filter-username username
    :hide-ownership-filter? true
    :default-list-mode :small
    :list-id [:charges-for-user-list username]}])

(defn- view-arms-for-user [username]
  [arms-select/component
   library.arms.list/on-select
   {:filter-username username
    :hide-ownership-filter? true
    :default-list-mode :small
    :list-id [:arms-for-user-list username]}])

(defn- view-collections-for-user [username]
  [collection-select/component
   library.collection.list/on-select
   {:filter-username username
    :hide-ownership-filter? true
    :default-list-mode :small
    :list-id [:collections-for-user-list username]}])

(defn- user-display [username]
  [status/default
   (rf/subscribe [::repository.user/data username])
   (fn [{user-info-data :user}]
     (let [username (:username user-info-data)]
       (rf/dispatch [::title/set username])
       [:div {:style {:display "grid"
                      :grid-gap "10px"
                      :grid-template-columns "[start] minmax(10em, 25%) [first] minmax(10em, 50%) [second] minmax(10em, 25%) [end]"
                      :grid-template-rows "[top] 100px [middle] auto [bottom]"
                      :grid-template-areas "'user-info user-info user-info'
                                       'collections arms charges'"
                      :padding-left "20px"
                      :padding-right "10px"
                      :height "100%"}}
        [:div.no-scrollbar {:style {:grid-area "user-info"
                                    :overflow-y "scroll"
                                    :padding-top "10px"}}
         [:img {:src (avatar/url username)
                :style {:width "80px"
                        :height "80px"}}]
         [:h2 {:style {:display "inline-block"
                       :vertical-align "top"
                       :margin-left "1em"}}
          (:username user-info-data)]]
        [:div.no-scrollbar {:style {:grid-area "collections"
                                    :overflow-y "scroll"}}
         [:h4 [tr :string.entity/collections]]
         [view-collections-for-user username]]
        [:div.no-scrollbar {:style {:grid-area "arms"
                                    :overflow-y "scroll"}}
         [:h4 [tr :string.entity/arms]]
         [view-arms-for-user username]]
        [:div.no-scrollbar {:style {:grid-area "charges"
                                    :overflow-y "scroll"}}
         [:h4 [tr :string.entity/charges]]
         [view-charges-for-user username]]]))])

(defn details-view [{{{:keys [username]} :path} :parameters}]
  [user-display username])

(defn- link-to-user [{:keys [username]}]
  [:a {:href (reife/href :route.user/details {:username username})}
   username])

(defn list-view []
  (rf/dispatch [::title/set :string.menu/users])
  [:div {:style {:padding "15px"}}
   (when (entity.user/admin? @(rf/subscribe [::session/data]))
     [status/default
      (rf/subscribe [::repository.user-list/data])
      (fn [{:keys [users]}]
        (into [:ul]
              (map (fn [user]
                     [:li (link-to-user user)]))
              (sort-by :username users)))])])
