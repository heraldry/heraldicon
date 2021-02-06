(ns heraldry.frontend.user-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.arms-library :as arms-library]
            [heraldry.frontend.charge-library :as charge-library]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]))

(def user-info-db-path
  [:user-info])

(defn fetch-user [username]
  (go
    (try
      (let [user-data (user/data)]
        (<? (api-request/call :fetch-user {:username username} user-data)))
      (catch :default e
        (println "fetch-user error:" e)))))

(defn view-charges-for-user [user-id]
  (let [[status charges] (state/async-fetch-data
                          [:my-charges]
                          user-id
                          #(charge-library/fetch-charges-for-user user-id))]
    (if (= status :done)
      [charge-library/show-charge-tree
       charges
       :remove-empty-groups? true
       :hide-access-filters? true]
      [:div "loading..."])))

(defn user-display []
  (let [user-info-data @(rf/subscribe [:get user-info-db-path])]
    [:<>
     [:div {:style {:padding-left "15px"}}
      [:h3 (str "User: " (:username user-info-data))]]
     [:div.pure-g
      [:div.pure-u-1-4
       [:div {:style {:padding-left "15px"}}
        [:h4 "Arms"]
        [arms-library/list-arms-for-user (:id user-info-data)]]]
      [:div.pure-u-3-4
       [:div {:style {:padding-left "15px"}}
        [:h4 "Charges"]
        [view-charges-for-user (:id user-info-data)]]]]]))

(defn view-user [username]
  (let [[status _user-form-data] (state/async-fetch-data
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
