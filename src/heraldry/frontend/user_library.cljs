(ns heraldry.frontend.user-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.arms-library :as arms-library]
            [heraldry.frontend.charge-library :as charge-library]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]))

(def user-info-db-path
  [:user-info])

(defn fetch-user-and-fill-info [username]
  (go
    (try
      (rf/dispatch-sync [:set user-info-db-path :loading])
      (let [user-data (user/data)
            response  (<? (api-request/call :fetch-user {:username username} user-data))]
        (rf/dispatch-sync [:set user-info-db-path response]))
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
  (let [user-info-data @(rf/subscribe [:get user-info-db-path])]
    (cond
      (and username
           (nil? user-info-data)) (do
                                    (fetch-user-and-fill-info username)
                                    [:<>])
      (= user-info-data :loading) [:<>]
      user-info-data              [user-display]
      :else                       [:<>])))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   "You need to be logged in."])

(defn view-user-by-username [{:keys [parameters]}]
  (let [user-data (user/data)
        username  (-> parameters :path :username)]
    (if (:logged-in? user-data)
      [view-user username]
      [not-logged-in])))
