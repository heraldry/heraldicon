(ns heraldicon.frontend.user.form.password-reset-confirmation
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as str]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.form.login :as-alias login]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private db-path
  [:ui :user-form :user :password-reset-confirmation])

(def ^:private db-path-step
  [:ui :user-form :password-reset-step])

(def ^:private db-path-code
  [:ui :user-form :password-reset-code])

(defn- code-form []
  [:form.modal-form {:autoComplete "off"
                     :on-submit (form/on-submit-fn [::submit-code])}
   [:p [tr :string.user.message/password-reset-confirmation-code-sent]]
   [message/display ::id]
   [form/text-field ::id :code :string.user/confirmation-code
    :label :string.user/confirmation-code]

   [:div {:style {:text-align "right"
                  :margin-top "10px"}}
    [:button.button
     {:style {:margin-right "5px"}
      :type "button"
      :on-click (form/on-submit-fn [::request-resend-code])}
     [tr :string.user.button/resend-code]]

    [:button.button.primary {:type "submit"}
     [tr :string.user.button/confirm]]]])

(defn- password-form []
  [:form.modal-form {:autoComplete "off"
                     :on-submit (form/on-submit-fn [::submit-password])}
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
     [tr :string.user.button/reset-password]]]])

(defn- form []
  (let [step @(rf/subscribe [:get db-path-step])]
    (if (= step :password)
      [password-form]
      [code-form])))

(rf/reg-event-fx ::show
  (fn [_ [_ username-or-email]]
    {:dispatch-n [[:set db-path username-or-email]
                  [:set db-path-step :code]
                  [:set db-path-code nil]
                  [::modal/create
                   :string.user/reset-forgotten-password
                   [form]
                   #(rf/dispatch [::form/clear ::id])]]}))

(rf/reg-event-fx ::submit-code
  (fn [{:keys [db]} _]
    (let [{:keys [code]} (form/data-from-db db ::id)
          username-or-email (get-in db db-path)]
      {:dispatch [::message/clear ::id]
       ::verify-code [username-or-email code]})))

(rf/reg-fx ::verify-code
  (fn [[username-or-email code]]
    (modal/start-loading)
    (go
      (try
        (<? (api/call :verify-confirmation-code {:username-or-email username-or-email
                                                 :code code
                                                 :type "password_reset"} nil))
        (rf/dispatch [::form/clear ::id])
        (rf/dispatch [:set db-path-code code])
        (rf/dispatch [:set db-path-step :password])
        (catch :default e
          (log/error e "verify code error")
          (rf/dispatch [::message/set-error ::id (:message (ex-data e))]))
        (finally
          (modal/stop-loading))))))

(rf/reg-event-fx ::request-resend-code
  (fn [{:keys [db]} _]
    (let [username-or-email (get-in db db-path)]
      {:dispatch [::message/clear ::id]
       ::resend-code [username-or-email]})))

(rf/reg-fx ::resend-code
  (fn [[username-or-email]]
    (modal/start-loading)
    (go
      (try
        (<? (api/call :start-password-reset {:username-or-email username-or-email} nil))
        (rf/dispatch [::message/set-success ::id :string.user.message/new-confirmation-code-sent])
        (catch :default e
          (log/error e "resend password reset code error")
          (rf/dispatch [::message/set-error ::id (:message (ex-data e))]))
        (finally
          (modal/stop-loading))))))

(rf/reg-event-fx ::submit-password
  (fn [{:keys [db]} _]
    (let [{:keys [new-password new-password-again]} (form/data-from-db db ::id)
          username-or-email (get-in db db-path)
          code (get-in db db-path-code)
          new-password? (not (str/blank? new-password))]
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
                new-password-again)) (assoc ::reset-password [username-or-email code new-password])))))

(rf/reg-fx ::reset-password
  (fn [[username-or-email code new-password]]
    (modal/start-loading)
    (go
      (try
        (<? (api/call :reset-password {:username-or-email username-or-email
                                       :code code
                                       :new-password new-password} nil))
        (rf/dispatch [::form/clear-and-close ::id])
        (rf/dispatch [::login/show
                      :string.user.message/password-reset-completed])
        (catch :default e
          (log/error e "password reset error")
          (rf/dispatch [::message/set-error ::id (:message (ex-data e))]))
        (finally
          (modal/stop-loading))))))
