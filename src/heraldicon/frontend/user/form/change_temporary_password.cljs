(ns heraldicon.frontend.user.form.change-temporary-password
  (:require
   [clojure.string :as s]
   [heraldicon.frontend.aws.cognito :as cognito]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.form.login :as login]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private db-path-user
  [:ui :user-form :user :password-reset-confirmation])

(defn- form []
  [:form.modal-form {:autoComplete "off"
                     :on-submit (form/on-submit-fn [::submit])}
   [:p [tr :string.user.message/password-reset-confirmation-code-sent]]
   [message/display ::id]
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
     [tr :string.user.button/change]]]])

(rf/reg-event-fx ::show
  (fn [_ [_ user user-attributes]]
    {:dispatch-n [[:set db-path-user {:user user
                                      :user-attributes user-attributes}]
                  [::modal/create
                   :string.user/change-temporary-password
                   [form]
                   #(rf/dispatch [::form/clear ::id])]]}))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} _]
    (let [{:keys [new-password new-password-again]} (form/data-from-db db ::id)
          {:keys [user user-attributes]} (get-in db db-path-user)
          new-password? (not (s/blank? new-password))]
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
                new-password-again)) (assoc ::change-password [user user-attributes new-password])))))

(rf/reg-fx ::change-password
  (fn [[user user-attributes new-password]]
    (modal/start-loading)
    (cognito/complete-new-password-challenge
     user new-password user-attributes
     :on-success (fn [^js/Object user]
                   (rf/dispatch [::form/clear-and-close ::id])
                   (login/login-with-token (-> user .getAccessToken .getJwtToken) :form-id ::id))
     :on-failure (fn [error]
                   (log/error "change password error:" error)
                   (rf/dispatch [::message/set-error ::id (.-message error)])
                   (modal/stop-loading)))))
