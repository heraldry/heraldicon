(ns heraldicon.frontend.account
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.user :as library.user]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   [tr :string.user.message/need-to-be-logged-in]])

(defn view []
  (let [{:keys [logged-in? username]} @(rf/subscribe [::session/data])]
    (if logged-in?
      [library.user/details-view {:parameters {:path {:username username}}}]
      [not-logged-in])))
