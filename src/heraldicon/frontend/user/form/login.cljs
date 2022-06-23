(ns heraldicon.frontend.user.form.login
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.form.core :as form]
   [re-frame.core :as rf]))

(defn- form []
  [:form.modal-form {:on-submit (form/on-submit-fn [::submit])}
   [message/display ::id]
   [form/text-field ::id :username :string.user/username]
   [form/password-field ::id :password :string.user/password]
   [:a {:style {:margin-right "5px"}
        :href "#"
        ;; TODO: forgot password clicked
        :on-click nil}
    [tr :string.user/forgotten-password]]

   [:div {:style {:text-align "right"
                  :margin-top "10px"}}
    [:button.button {:style {:margin-right "5px"}
                     :type "reset"
                     :on-click #(rf/dispatch [::form/cancel ::id])}
     [tr :string.button/cancel]]

    [:button.button.primary {:type "submit"}
     [tr :string.menu/login]]]])

(rf/reg-event-fx ::show
  (fn [_ [_ title]]
    {:dispatch [::modal/create
                (or title :string.menu/login)
                [form]
                #(rf/dispatch [::form/clear ::id])]}))
