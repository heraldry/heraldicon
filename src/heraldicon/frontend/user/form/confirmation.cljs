(ns heraldicon.frontend.user.form.confirmation
  (:require
   [heraldicon.frontend.aws.cognito :as cognito]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.user.form.core :as form]
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
  (fn [_ [_ user]]
    {:dispatch-n [[:set db-path-user user]
                  [::modal/create
                   :string.user/register-confirmation
                   [form]
                   #(rf/dispatch [::form/clear ::id])]]}))

(rf/reg-event-fx ::submit
  (fn [{:keys [db]} _]
    (let [{:keys [code]} (form/data-from-db db ::id)
          user (get-in db db-path-user)]
      {:dispatch [::message/clear ::id]
       ::confirm [user code]})))

(rf/reg-fx ::confirm
  (fn [[user code]]
    (modal/start-loading)
    (cognito/confirm
     user code
     :on-success (fn [_user]
                   (rf/dispatch [::form/clear-and-close ::id])
                   (rf/dispatch [:heraldicon.frontend.user.form.login/show
                                 :string.user.message/registration-completed])
                   (modal/stop-loading))
     :on-failure (fn [error]
                   (log/error "confirmation error:" error)
                   (rf/dispatch [::message/set-error ::id (.-message error)])
                   (modal/stop-loading)))))

(rf/reg-event-fx ::request-resend-code
  (fn [{:keys [db]} _]
    (let [user (get-in db db-path-user)]
      {:dispatch [::message/clear ::id]
       ::resend-code [user]})))

(rf/reg-fx ::resend-code
  (fn [[user]]
    (modal/start-loading)
    (cognito/resend-code
     user
     :on-success (fn [_user]
                   (rf/dispatch [::message/set-success ::id :string.user.message/new-confirmation-code-sent])
                   (modal/stop-loading))
     :on-failure (fn [error]
                   (log/error "resend code error:" error)
                   (rf/dispatch [::message/set-error ::id (.-message error)])
                   (modal/stop-loading)))))
