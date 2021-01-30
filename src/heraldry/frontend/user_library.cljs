(ns heraldry.frontend.user-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
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

(defn user-display []
  (let [charge-info-data @(rf/subscribe [:get user-info-db-path])]
    [:<>
     [:div {:style {:padding-left "15px"}}
      [:h3 (str "User: " (:username charge-info-data))]]
     [:div.pure-g
      [:div.pure-u-1-2 {:style {:position "fixed"}}
       [:div {:style {:padding-left "15px"}}
        [:h4 "Arms"]
        [arms-library/list-arms-for-user (:id charge-info-data)]]]
      [:div.pure-u-1-2 {:style {:margin-left "50%"
                                :width       "45%"}}
       [:div {:style {:padding-left "15px"}}
        [:h4 "Charges"]
        [charge-library/list-charges-for-user (:id charge-info-data)]]]]]))

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
  (let [user-data (user/data)
        username  (-> parameters :path :username)]
    (if (:logged-in? user-data)
      [view-user username]
      [not-logged-in])))
