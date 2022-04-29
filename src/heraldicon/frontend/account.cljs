(ns heraldicon.frontend.account
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.user :as user]
   [heraldicon.frontend.user-library :as user-library]))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   [tr :string.user.message/need-to-be-logged-in]])

(defn view []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [user-library/view-user (:username user-data)]
      [not-logged-in])))
