(ns heraldicon.frontend.user-library
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.config :as config]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.arms-library :as arms-library]
   [heraldicon.frontend.charge :as charge]
   [heraldicon.frontend.charge-library :as charge-library]
   [heraldicon.frontend.collection-library :as collection-library]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.arms-select :as arms-select]
   [heraldicon.frontend.ui.element.charge-select :as charge-select]
   [heraldicon.frontend.ui.element.collection-select :as collection-select]
   [heraldicon.frontend.ui.element.user-select :as user-select]
   [heraldicon.frontend.user :as user]
   [heraldicon.util :as util]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(def user-info-db-path
  [:user-info])

(defn fetch-user [username]
  (go
    (try
      (let [user-data (user/data)]
        (<? (api.request/call :fetch-user {:username username} user-data)))
      (catch :default e
        (log/error "fetch user error:" e)))))

(defn invalidate-charges-cache-for-user [user-id]
  (state/invalidate-cache [:user-charges] user-id))

(defn view-charges-for-user [user-id]
  (let [[status _charges] (state/async-fetch-data
                           [:user-charges]
                           user-id
                           #(charge/fetch-charges-for-user user-id))]
    (if (= status :done)
      [charge-select/component
       [:user-charges]
       charge-library/on-select
       #(invalidate-charges-cache-for-user user-id)
       :remove-empty-groups? true
       :hide-ownership-filter? true]
      [:div [tr :string.miscellaneous/loading]])))

(defn invalidate-arms-cache-for-user [user-id]
  (state/invalidate-cache [:user-arms] user-id))

(defn view-arms-for-user [user-id]
  (let [[status _arms-list] (state/async-fetch-data
                             [:user-arms]
                             user-id
                             #(arms-select/fetch-arms-list-by-user user-id))]
    (if (= status :done)
      [arms-select/component
       [:user-arms]
       arms-library/on-select
       #(invalidate-arms-cache-for-user user-id)
       :hide-ownership-filter? true]
      [:div [tr :string.miscellaneous/loading]])))

(defn invalidate-collection-cache-for-user [user-id]
  (state/invalidate-cache [:user-collections] user-id))

(defn view-collections-for-user [user-id]
  (let [[status collection-list] (state/async-fetch-data
                                  [:user-collections]
                                  user-id
                                  #(collection-select/fetch-collection-list-by-user user-id))]
    (if (= status :done)
      [collection-select/component
       collection-list
       collection-library/link-to-collection
       #(invalidate-collection-cache-for-user user-id)
       :hide-ownership-filter? true]
      [:div [tr :string.miscellaneous/loading]])))

(defn user-display []
  (let [user-info-data @(rf/subscribe [:get user-info-db-path])
        user-id (:id user-info-data)]
    (rf/dispatch [:set-title (:username user-info-data)])
    [:div {:style {:display "grid"
                   :grid-gap "10px"
                   :grid-template-columns "[start] 33% [first] auto [second] 25% [end]"
                   :grid-template-rows "[top] 100px [middle] auto [bottom]"
                   :grid-template-areas "'user-info user-info user-info'
                                       'collections arms charges'"
                   :padding-left "20px"
                   :padding-right "10px"
                   :height "100%"}
           :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
     [:div.no-scrollbar {:style {:grid-area "user-info"
                                 :overflow-y "scroll"
                                 :padding-top "10px"}}
      [:img {:src (util/avatar-url (:username user-info-data))
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
      [view-charges-for-user user-id]]]))

(defn view-user [username]
  (let [[status _userform-data] (state/async-fetch-data
                                 user-info-db-path
                                 username
                                 #(fetch-user username))]
    (when (= status :done)
      [user-display])))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   [tr :string.user.message/need-to-be-logged-in]])

(defn view-user-by-username [{:keys [parameters]}]
  (let [username (-> parameters :path :username)]
    [view-user username]))

(defn link-to-user [{:keys [username]}]
  [:a {:href (reife/href :view-user {:username username})}
   username])

(defn list-all-users []
  (when (-> (user/data) :username ((config/get :admins)))
    [user-select/list-users link-to-user]))

(defn view-list-users []
  (rf/dispatch [:set-title :string.menu/users])
  [:div {:style {:padding "15px"}}
   [list-all-users]])
