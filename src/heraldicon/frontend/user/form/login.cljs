(ns heraldicon.frontend.user.form.login
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.aws.cognito :as cognito]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.repository.api :as api]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.form.password-reset-confirmation :as password-reset-confirmation]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn- form []
  [:form.modal-form {:on-submit (form/on-submit-fn [::submit])}
   [message/display ::id]
   [form/text-field ::id :username :string.user/username]
   [form/password-field ::id :password :string.user/password]
   [:a {:style {:margin-right "5px"}
        :href "#"
        :on-click (form/on-submit-fn [::forgot-password-clicked])}
    [tr :string.user/forgotten-password]]

   [:div {:style {:text-align "right"
                  :margin-top "10px"}}
    [:button.button {:style {:margin-right "5px"}
                     :type "reset"
                     :on-click #(rf/dispatch [::form/clear-and-close ::id])}
     [tr :string.button/cancel]]

    [:button.button.primary {:type "submit"}
     [tr :string.menu/login]]]])

(rf/reg-event-fx ::show
  (fn [_ [_ title]]
    {:dispatch [::modal/create
                (or title :string.menu/login)
                [form]
                #(rf/dispatch [::form/clear ::id])]}))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} _]
    (let [{:keys [username password]} (form/data-from-db db ::id)
          username? (-> username count pos?)
          password? (-> password count pos?)]
      (cond-> {:dispatch-n [[::message/clear ::id]]}

        (not username?) (update :dispatch-n conj
                                [::message/set-error (form/message-id ::id :username)
                                 :string.user.message/username-required])
        (not password?) (update :dispatch-n conj
                                [::message/set-error (form/message-id ::id :password)
                                 :string.user.message/password-required])
        (and username?
             password?) (assoc ::login [username password])))))

(defn- clear-list-repositories []
  (rf/dispatch [:heraldicon.frontend.repository.entity-list/clear-all])
  (rf/dispatch [:heraldicon.frontend.repository.user-list/clear]))

(defn login-with-token [jwt-token & {:keys [form-id]
                                     :or {form-id ::id}}]
  (go
    (try
      (let [session-data (<? (api/call :login {:jwt-token jwt-token} nil))]
        (clear-list-repositories)
        (rf/dispatch [::session/store session-data])
        (rf/dispatch [::form/clear-and-close form-id]))
      (catch :default e
        (log/error "login with token error:" e)
        (rf/dispatch [::message/set-error form-id (:message e)]))
      (finally
        (modal/stop-loading)))))

(rf/reg-fx ::login
  (fn [[username password]]
    (modal/start-loading)
    (cognito/login
     username password
     :on-success (fn [^js/Object user]
                   (login-with-token (-> user .getAccessToken .getJwtToken)))
     :on-confirmation-needed (fn [user]
                               (rf/dispatch [::form/clear-and-close ::id])
                               (rf/dispatch [:heraldicon.frontend.user.form.confirmation/show user])
                               (modal/stop-loading))
     :on-failure (fn [error]
                   (log/error "login error:" error)
                   (rf/dispatch [::message/set-error ::id (.-message error)])
                   (modal/stop-loading))
     :on-new-password-required (fn [user user-attributes]
                                 (rf/dispatch [::form/clear-and-close ::id])
                                 (rf/dispatch [:heraldicon.frontend.user.form.change-temporary-password/show user user-attributes])
                                 (modal/stop-loading)))))

(rf/reg-fx ::start-password-reset
  (fn [[username]]
    (modal/start-loading)
    (cognito/forgot-password
     username
     :on-success (fn [user]
                   (rf/dispatch [::form/clear-and-close ::id])
                   (rf/dispatch [::password-reset-confirmation/show user])
                   (modal/stop-loading))
     :on-failure (fn [error]
                   (log/error "password reset initiation error:" error)
                   (rf/dispatch [::message/set-error ::id (.-message error)])
                   (modal/stop-loading)))))

(rf/reg-event-fx ::forgot-password-clicked
  (fn [{:keys [db]} _]
    (let [{:keys [username]} (form/data-from-db db ::id)
          username? (-> username count pos?)]
      (cond-> {:dispatch-n [[::message/clear ::id]]}

        (not username?) (update :dispatch-n conj
                                [::message/set-error (form/message-id ::id :username)
                                 :string.user.message/username-required])

        username? (assoc ::start-password-reset [username])))))
