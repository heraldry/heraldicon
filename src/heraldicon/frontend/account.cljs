(ns heraldicon.frontend.account
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.user :as library.user]
   [heraldicon.frontend.user :as user]))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   [tr :string.user.message/need-to-be-logged-in]])

(defn view []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [library.user/view-user (:username user-data)]
      [not-logged-in])))
