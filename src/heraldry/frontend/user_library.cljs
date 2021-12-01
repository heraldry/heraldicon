(ns heraldry.frontend.user-library
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.config :as config]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.arms-library :as arms-library]
   [heraldry.frontend.charge :as charge]
   [heraldry.frontend.charge-library :as charge-library]
   [heraldry.frontend.collection-library :as collection-library]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.arms-select :as arms-select]
   [heraldry.frontend.ui.element.charge-select :as charge-select]
   [heraldry.frontend.ui.element.collection-select :as collection-select]
   [heraldry.frontend.ui.element.user-select :as user-select]
   [heraldry.frontend.user :as user]
   [heraldry.gettext :refer [string]]
   [heraldry.util :as util]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(def user-info-db-path
  [:user-info])

(defn fetch-user [username]
  (go
    (try
      (let [user-data (user/data)]
        (<? (api-request/call :fetch-user {:username username} user-data)))
      (catch :default e
        (log/error "fetch user error:" e)))))

(defn invalidate-charges-cache-for-user [user-id]
  (state/invalidate-cache [:user-charges] user-id))

(defn view-charges-for-user [user-id]
  (let [[status charges] (state/async-fetch-data
                          [:user-charges]
                          user-id
                          #(charge/fetch-charges-for-user user-id))]
    (if (= status :done)
      [charge-select/component
       charges
       charge-library/link-to-charge
       #(invalidate-charges-cache-for-user user-id)
       :remove-empty-groups? true
       :hide-ownership-filter? true]
      [:div [tr (string "Loading...")]])))

(defn invalidate-arms-cache-for-user [user-id]
  (state/invalidate-cache [:user-arms] user-id))

(defn view-arms-for-user [user-id]
  (let [[status arms-list] (state/async-fetch-data
                            [:user-arms]
                            user-id
                            #(arms-select/fetch-arms-list-by-user user-id))]
    (if (= status :done)
      [arms-select/component
       arms-list
       arms-library/link-to-arms
       #(invalidate-arms-cache-for-user user-id)
       :hide-ownership-filter? true]
      [:div [tr (string "Loading...")]])))

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
      [:div [tr (string "Loading...")]])))

(defn user-display []
  (let [user-info-data @(rf/subscribe [:get user-info-db-path])
        user-id (:id user-info-data)]
    (rf/dispatch [:set-title (:username user-info-data)])
    [:div {:style {:display "grid"
                   :grid-gap "10px"
                   :grid-template-columns "[start] 33% [first] auto [second] 25% [end]"
                   :grid-template-rows "[top] 3em [middle] auto [bottom]"
                   :grid-template-areas "'user-info user-info user-info'
                                       'collections arms charges'"
                   :padding-left "20px"
                   :padding-right "10px"
                   :height "100%"}
           :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
     [:div.no-scrollbar {:style {:grid-area "user-info"
                                 :overflow-y "scroll"}}
      [:h3 [tr (util/str-tr (string "User") ": " (:username user-info-data))]]]
     [:div.no-scrollbar {:style {:grid-area "collections"
                                 :overflow-y "scroll"}}
      [:h4 [tr (string "Collections")]]
      [view-collections-for-user user-id]]
     [:div.no-scrollbar {:style {:grid-area "arms"
                                 :overflow-y "scroll"}}
      [:h4 [tr (string "Arms")]]
      [view-arms-for-user user-id]]
     [:div.no-scrollbar {:style {:grid-area "charges"
                                 :overflow-y "scroll"}}
      [:h4 [tr (string "Charges")]]
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
   "You need to be logged in."])

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
  (rf/dispatch [:set-title (string "Users")])
  [:div {:style {:padding "15px"}}
   [list-all-users]])
