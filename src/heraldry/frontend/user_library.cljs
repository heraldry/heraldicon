(ns heraldry.frontend.user-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.arms-library :as arms-library]
            [heraldry.frontend.charge :as charge]
            [heraldry.frontend.charge-library :as charge-library]
            [heraldry.frontend.collection-library :as collection-library]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.arms-select :as arms-select]
            [heraldry.frontend.ui.element.charge-select :as charge-select]
            [heraldry.frontend.ui.element.collection-select :as collection-select]
            [heraldry.frontend.ui.element.user-select :as user-select]
            [heraldry.frontend.user :as user]
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
      [:div "loading..."])))

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
      [:div "loading..."])))

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
      [:div "loading..."])))

(defn user-display []
  (let [user-info-data @(rf/subscribe [:get user-info-db-path])
        user-id (:id user-info-data)]
    [:<>
     [:div {:style {:padding-left "15px"}}
      [:h3 (str "User: " (:username user-info-data))]]
     [:div.pure-g
      [:div.pure-u-1-3
       [:div {:style {:padding-left "15px"}}
        [:h4 "Collections"]
        [view-collections-for-user user-id]]]
      [:div.pure-u-1-3
       [:div {:style {:padding-left "15px"}}
        [:h4 "Arms"]
        [view-arms-for-user user-id]]]
      [:div.pure-u-1-3
       [:div {:style {:padding-left "15px"}}
        [:h4 "Charges"]
        [view-charges-for-user user-id]]]]]))

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
  [user-select/list-users link-to-user])

(defn view-list-users []
  [:div {:style {:padding "15px"}}
   [:div.pure-u-1-2 {:style {:display "block"
                             :text-align "justify"
                             :min-width "30em"}}

    [:div {:style {:padding-top "0.5em"}}
     [list-all-users]]]])
