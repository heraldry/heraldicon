(ns heraldicon.frontend.user.form.confirmation
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.form.login :as-alias login]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def ^:private db-path-user
  [:ui :user-form :user :confirmation])

(defn- form []
  [:form.modal-form {:autoComplete "off"
                     :on-submit (form/on-submit-fn [::submit])}
   [:p [tr :string.user.message/confirmation-code-sent]]
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

(rf/reg-event-fx ::show
  (fn [_ [_ username-or-email]]
    {:dispatch-n [[:set db-path-user username-or-email]
                  [::modal/create
                   :string.user/register-confirmation
                   [form]
                   #(rf/dispatch [::form/clear ::id])]]}))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} _]
    (let [{:keys [code]} (form/data-from-db db ::id)
          username-or-email (get-in db db-path-user)]
      {:dispatch [::message/clear ::id]
       ::confirm [username-or-email code]})))

(rf/reg-fx ::confirm
  (fn [[username-or-email code]]
    (modal/start-loading)
    (go
      (try
        (<? (api/call :confirm-account {:username-or-email username-or-email
                                        :code code} nil))
        (rf/dispatch [::form/clear-and-close ::id])
        (rf/dispatch [::login/show :string.user.message/registration-completed])
        (catch :default e
          (log/error e "confirmation error")
          (rf/dispatch [::message/set-error ::id (:message (ex-data e))]))
        (finally
          (modal/stop-loading))))))

(rf/reg-event-fx ::request-resend-code
  (fn [{:keys [db]} _]
    (let [username-or-email (get-in db db-path-user)]
      {:dispatch [::message/clear ::id]
       ::resend-code [username-or-email]})))

(rf/reg-fx ::resend-code
  (fn [[username-or-email]]
    (modal/start-loading)
    (go
      (try
        (<? (api/call :resend-confirmation-code {:username-or-email username-or-email} nil))
        (rf/dispatch [::message/set-success ::id :string.user.message/new-confirmation-code-sent])
        (catch :default e
          (log/error e "resend code error")
          (rf/dispatch [::message/set-error ::id (:message (ex-data e))]))
        (finally
          (modal/stop-loading))))))
