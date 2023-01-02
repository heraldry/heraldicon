(ns heraldicon.frontend.user.form.register
  (:require [clojure.string :as s]
            [heraldicon.frontend.aws.cognito :as cognito]
            [heraldicon.frontend.language :refer [tr]]
            [heraldicon.frontend.message :as message]
            [heraldicon.frontend.modal :as modal]
            [heraldicon.frontend.user.form.confirmation :as confirmation]
            [heraldicon.frontend.user.form.core :as form]
            [heraldicon.frontend.user.form.login :as-alias login]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

(defn- form []
  [:form.modal-form {:on-submit (form/on-submit-fn [::submit])}
   [message/display ::id]
   [form/text-field ::id :username :string.user/username
    :label :string.user/username]
   [form/text-field ::id :email :string.user/email
    :label :string.user/email]
   [form/password-field ::id :password :string.user/password
    :label :string.user/password]
   [form/password-field ::id :password-again :string.user/password-again
    :label :string.user/password-again]

   [:div {:style {:text-align "right"
                  :margin-top "10px"}}
    [:button.button {:style {:margin-right "5px"}
                     :type "reset"
                     :on-click #(rf/dispatch [::form/clear-and-close ::id])}
     [tr :string.button/cancel]]

    [:button.button.primary {:type "submit"}
     [tr :string.menu/register]]]])

(rf/reg-event-fx ::show
  (fn [_ _]
    {:dispatch [::modal/create
                :string.menu/register
                [form]
                #(rf/dispatch [::form/clear ::id])]}))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} _]
    (let [{:keys [username email password password-again]} (form/data-from-db db ::id)
          username? (not (s/blank? username))
          email? (not (s/blank? email))
          password? (not (s/blank? password))]
      (cond-> {:dispatch-n [[::message/clear ::id]]}

        (not username?) (update :dispatch-n conj
                                [::message/set-error (form/message-id ::id :username)
                                 :string.user.message/username-required])
        (not email?) (update :dispatch-n conj
                             [::message/set-error (form/message-id ::id :email)
                              :string.user.message/email-required])
        (not password?) (update :dispatch-n conj
                                [::message/set-error (form/message-id ::id :password)
                                 :string.user.message/password-required])
        (not= password
              password-again) (update :dispatch-n conj
                                      [::message/set-error (form/message-id ::id :password-again)
                                       :string.user.message/passwords-do-not-match])
        (and username?
             email?
             password?
             (= password
                password-again)) (assoc ::register [username email password])))))

(rf/reg-fx ::register
  (fn [[username email password]]
    (modal/start-loading)
    (cognito/sign-up
     username password email
     :on-success (fn [_user]
                   (rf/dispatch [::form/clear-and-close ::id])
                   (rf/dispatch [::login/show
                                 :string.user.message/registration-completed])
                   (modal/stop-loading))
     :on-confirmation-needed (fn [user]
                               (rf/dispatch [::form/clear-and-close ::id])
                               (rf/dispatch [::confirmation/show user])
                               (modal/stop-loading))
     :on-failure (fn [error]
                   (log/error "sign-up error" error)
                   (rf/dispatch [::message/set-error ::id (.-message error)])
                   (modal/stop-loading)))))
