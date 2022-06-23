(ns heraldicon.frontend.user.form.password-reset-confirmation
  (:require
   [heraldicon.frontend.aws.cognito :as cognito]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.form.core :as form]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private db-path
  [:ui :user-form :user :password-reset-confirmation])

(defn- form []
  [:form.modal-form {:autoComplete "off"
                     :on-submit (form/on-submit-fn [::submit])}
   [:p [tr :string.user.message/password-reset-confirmation-code-sent]]
   [message/display ::id]
   [form/text-field ::id :code :string.user/confirmation-code
    :label :string.user/confirmation-code]
   [form/password-field ::id :new-password :string.user/new-password
    :label :string.user/new-password]
   [form/password-field ::id :new-password-again :string.user/new-password-again
    :label :string.user/new-password-again]

   [:div {:style {:text-align "right"
                  :margin-top "10px"}}
    [:button.button {:style {:margin-right "5px"}
                     :type "reset"
                     :on-click #(rf/dispatch [::form/clear-and-close ::id])}
     [tr :string.button/cancel]]

    [:button.button.primary {:type "submit"}
     [tr :string.user.button/reset-password]]]])

(rf/reg-event-fx ::show
  (fn [_ [_ user]]
    {:dispatch-n [[:set db-path user]
                  [::modal/create
                   :string.user/reset-forgotten-password
                   [form]
                   #(rf/dispatch [::form/clear ::id])]]}))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} _]
    (let [{:keys [code new-password new-password-again]} (form/data-from-db db ::id)
          user (get-in db db-path)
          new-password? (-> new-password count pos?)]
      (cond-> {:dispatch-n [[::message/clear ::id]]}

        (not new-password?) (update :dispatch-n conj
                                    [::message/set-error (form/message-id ::id :new-password)
                                     :string.user.message/password-required])
        (not= new-password
              new-password-again) (update :dispatch-n conj
                                          [::message/set-error (form/message-id ::id :new-password-again)
                                           :string.user.message/passwords-do-not-match])
        (and new-password?
             (= new-password
                new-password-again)) (assoc ::reset-password [user code new-password])))))

(rf/reg-fx ::reset-password
  (fn [[user code new-password]]
    (modal/start-loading)
    (cognito/confirm-password
     user code new-password
     :on-success (fn [_user]
                   (rf/dispatch [::form/clear-and-close ::id])
                   (rf/dispatch [:heraldicon.frontend.user.form.login/show
                                 :string.user.message/password-reset-completed])
                   (modal/stop-loading))
     :on-failure (fn [error]
                   (log/error "password reset error:" error)
                   (rf/dispatch [::message/set-error ::id (.-message error)])
                   (modal/stop-loading)))))
