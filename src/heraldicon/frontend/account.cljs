(ns heraldicon.frontend.account
  (:require
   [heraldicon.avatar :as avatar]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.library.user :as library.user]
   [heraldicon.frontend.repository.user :as repository.user]
   [heraldicon.frontend.user.form.avatar :as form.avatar]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   [tr :string.user.message/need-to-be-logged-in]])

(defn- avatar-strip [username]
  (let [{user-info :user} @(rf/subscribe [::repository.user/data username])
        has-avatar? (some? (:avatar-key user-info))]
    [:div {:style {:display "flex"
                   :align-items "center"
                   :gap "15px"
                   :padding "10px 20px"}}
     (when (:avatar-url user-info)
       [:img {:src (:avatar-url user-info)
              :style (merge {:width "80px"
                             :height "80px"}
                            (avatar/shape-style (:uncropped-avatar? user-info)))}])
     [:button.button {:on-click #(rf/dispatch [::form.avatar/show])
                      :style {:width "auto"}}
      [tr :string.user.avatar/upload]]
     (when has-avatar?
       [:button.button.danger {:on-click #(rf/dispatch [::form.avatar/remove])
                               :style {:width "auto"}}
        [tr :string.user.avatar/remove]])]))

(defn view []
  (let [{:keys [username]} @(rf/subscribe [::session/data])]
    (if @(rf/subscribe [::session/logged-in?])
      [:<>
       [avatar-strip username]
       [library.user/details-view {:parameters {:path {:username username}}}]]
      [not-logged-in])))
