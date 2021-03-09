(ns heraldry.frontend.user
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.aws.cognito :as cognito]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.state :as state]
            [hodgepodge.core :refer [local-storage get-item remove-item set-item]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

(def user-db-path [:user-data])

(def local-storage-session-id-name
  "cl-session-id")

(def local-storage-user-id-name
  "cl-user-id")

(def local-storage-username-name
  "cl-username")

(declare login-modal)
(declare confirmation-modal)
(declare change-temporary-password-modal)
(declare password-reset-confirmation-modal)

(defn data []
  @(rf/subscribe [:get user-db-path]))

(defn read-session-data []
  (let [session-id (get-item local-storage local-storage-session-id-name)
        user-id (get-item local-storage local-storage-user-id-name)
        username (get-item local-storage local-storage-username-name)]
    (rf/dispatch-sync [:set user-db-path
                       (if (and session-id username user-id)
                         {:username username
                          :session-id session-id
                          :user-id user-id
                          :logged-in? true}
                         nil)])))

(defn complete-login [db-path jwt-token]
  (go
    (try
      (let [response (<? (api-request/call :login {:jwt-token jwt-token} nil))
            {:keys [session-id
                    username
                    user-id]} response]
        (set-item local-storage local-storage-session-id-name session-id)
        (set-item local-storage local-storage-username-name username)
        (set-item local-storage local-storage-user-id-name user-id)
        (read-session-data)
        (rf/dispatch-sync [:clear-form db-path])
        (state/invalidate-cache-all)
        (modal/clear))
      (catch :default e
        (log/error "complete login error:" e)
        (rf/dispatch [:set-form-error db-path (:message e)])))))

(defn login-clicked [db-path]
  (let [{:keys [username password]} @(rf/subscribe [:get db-path])]
    (rf/dispatch-sync [:clear-form-errors db-path])
    (cognito/login username
                   password
                   :on-success (fn [user]
                                 (complete-login db-path (-> user
                                                             .getAccessToken
                                                             .getJwtToken)))
                   :on-confirmation-needed (fn [user]
                                             (rf/dispatch [:clear-form db-path])
                                             (rf/dispatch [:set (conj user-db-path :user) user])
                                             (confirmation-modal))
                   :on-failure (fn [error]
                                 (log/error "login error:" error)
                                 (rf/dispatch [:set-form-error db-path (:message error)]))
                   :on-new-password-required (fn [user user-attributes]
                                               (rf/dispatch [:clear-form db-path])
                                               (rf/dispatch [:set (conj user-db-path :user) user])
                                               (rf/dispatch [:set (conj user-db-path :user-attributes) user-attributes])
                                               (change-temporary-password-modal)))))
(defn forgotten-password-clicked [db-path]
  (let [{:keys [username]} @(rf/subscribe [:get db-path])]
    (rf/dispatch-sync [:clear-form-errors db-path])
    (if (-> username
            count
            zero?)
      (rf/dispatch [:set-form-error (conj db-path :username) "Username required for password reset."])
      (cognito/forgot-password
       username
       :on-success (fn [user]
                     (rf/dispatch [:clear-form db-path])
                     (rf/dispatch [:set (conj user-db-path :user) user])
                     (password-reset-confirmation-modal))
       :on-failure (fn [error]
                     (log/error "password reset initiation error:" error)
                     (rf/dispatch [:set-form-error db-path (:message error)]))))))

(defn login-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (login-clicked db-path))]
    [:form.pure-form.pure-form-stacked
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form/field (conj db-path :username)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:input {:id "username"
                   :value value
                   :on-change on-change
                   :placeholder "Username"
                   :type "text"}]])]
      [form/field (conj db-path :password)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:input {:id "password"
                   :value value
                   :on-change on-change
                   :placeholder "Password"
                   :type "password"}]])]
      [:a
       {:style {:margin-right "5px"}
        :href "#"
        :on-click (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (forgotten-password-clicked db-path))}
       "Forgotten password"]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button
        {:style {:margin-right "5px"}
         :type "reset"
         :on-click #(do
                      (rf/dispatch [:clear-form db-path])
                      (modal/clear))}
        "Cancel"]
       [:button.pure-button.pure-button-primary {:type "submit"}
        "Login"]]]]))

(defn sign-up-clicked [db-path]
  (let [{:keys [username email password password-again]} @(rf/subscribe [:get db-path])]
    (rf/dispatch-sync [:clear-form-errors])
    (if (not= password password-again)
      (rf/dispatch [:set-form-error (conj db-path :password-again) "Passwords don't match."])
      (cognito/sign-up username password email
                       :on-success (fn [_user]
                                     (rf/dispatch [:clear-form db-path])
                                     (login-modal "Registration completed"))
                       :on-confirmation-needed (fn [user]
                                                 (rf/dispatch [:clear-form db-path])
                                                 (rf/dispatch [:set (conj user-db-path :user) user])
                                                 (confirmation-modal))
                       :on-failure (fn [error]
                                     (log/error "sign-up error" error)
                                     (rf/dispatch [:set-form-error db-path (:message error)]))))))

(defn sign-up-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (sign-up-clicked db-path))]
    [:form.pure-form.pure-form-aligned
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form/field (conj db-path :username)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "username"} "Username"]
          [:input {:id "username"
                   :value value
                   :on-change on-change
                   :placeholder "Username"
                   :type "text"}]])]
      [form/field (conj db-path :email)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "email"} "Email"]
          [:input {:id "email"
                   :value value
                   :on-change on-change
                   :placeholder "Email"
                   :type "text"}]])]
      [form/field (conj db-path :password)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "password"} "Password:"]
          [:input {:id "password"
                   :value value
                   :on-change on-change
                   :placeholder "Password"
                   :type "password"}]])]
      [form/field (conj db-path :password-again)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "password-again"} "Password again:"]
          [:input {:id "password-again"
                   :value value
                   :on-change on-change
                   :placeholder "Password again"
                   :type "password"}]])]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button
        {:style {:margin-right "5px"}
         :type "reset"
         :on-click #(do
                      (rf/dispatch [:clear-form db-path])
                      (modal/clear))}
        "Cancel"]
       [:button.pure-button.pure-button-primary {:type "submit"}
        "Register"]]]]))

(defn confirm-clicked [db-path]
  (let [{:keys [code]} @(rf/subscribe [:get db-path])
        user-data (data)
        user (:user user-data)]
    (rf/dispatch-sync [:clear-form-errors db-path])
    (cognito/confirm user code
                     :on-success (fn [_user]
                                   (rf/dispatch [:clear-form db-path])
                                   (login-modal "Registration completed"))
                     :on-failure (fn [error]
                                   (log/error "confirmation error:" error)
                                   (rf/dispatch [:set-form-error db-path (:message error)])))))

(defn resend-code-clicked [db-path]
  (let [user-data (data)
        user (:user user-data)]
    (cognito/resend-code user
                         :on-success (fn []
                                       (js/alert "A new code was sent to your email address."))
                         :on-failure (fn [error]
                                       (log/error "resend code error:" error)
                                       (rf/dispatch [:set-form-error db-path (:message error)])))))

(defn confirmation-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (confirm-clicked db-path))]
    [:form.pure-form.pure-form-stacked
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     "A confirmation code was sent to your email address."
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form/field (conj db-path :code)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:input {:id "code"
                   :value value
                   :on-change on-change
                   :placeholder "Confirmation code"
                   :type "text"}]])]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button
        {:style {:margin-right "5px"}
         :type "button"
         :on-click #(resend-code-clicked db-path)}
        "Resend code"]
       [:button.pure-button.pure-button-primary {:type "submit"} "Confirm"]]]]))

(defn change-temporary-password-clicked [db-path]
  (let [user-data (data)
        user (:user user-data)
        user-attributes (:user-attributes user-data)
        {:keys [new-password
                new-password-again]} @(rf/subscribe [:get db-path])]
    (rf/dispatch-sync [:clear-form-errors])
    (if (not= new-password new-password-again)
      (rf/dispatch [:set-form-error (conj db-path :new-password-again) "Passwords don't match."])
      (cognito/complete-new-password-challenge
       user
       new-password
       user-attributes
       :on-success (fn [user]
                     (rf/dispatch [:clear-form db-path])
                     (complete-login db-path (-> user
                                                 .getAccessToken
                                                 .getJwtToken)))
       :on-failure (fn [error]
                     (log/error "change password error:" error)
                     (rf/dispatch [:set-form-error db-path (:message error)]))))))

(defn change-temporary-password-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (change-temporary-password-clicked db-path))]
    [:form.pure-form.pure-form-aligned
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form/field (conj db-path :new-password)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "new-password"} "New password:"]
          [:input {:id "new-password"
                   :value value
                   :on-change on-change
                   :placeholder "New password"
                   :type "password"}]])]
      [form/field (conj db-path :new-password-again)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "new-password-again"} "New password again:"]
          [:input {:id "new-password-again"
                   :value value
                   :on-change on-change
                   :placeholder "New password again"
                   :type "password"}]])]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button
        {:style {:margin-right "5px"}
         :type "reset"
         :on-click #(do
                      (rf/dispatch [:clear-form db-path])
                      (modal/clear))}
        "Cancel"]
       [:button.pure-button.pure-button-primary {:type "submit"}
        "Change"]]]]))

(defn reset-password-clicked [db-path]
  (let [{:keys [code
                new-password
                new-password-again]} @(rf/subscribe [:get db-path])
        user-data (data)
        user (:user user-data)]
    (rf/dispatch-sync [:clear-form-errors db-path])
    (if (not= new-password new-password-again)
      (rf/dispatch [:set-form-error (conj db-path :new-password-again) "Passwords don't match."])
      (cognito/confirm-password
       user
       code
       new-password
       :on-success (fn [_user]
                     (rf/dispatch [:clear-form db-path])
                     (login-modal "Password reset completed"))
       :on-failure (fn [error]
                     (log/error "password reset error:" error)
                     (rf/dispatch [:set-form-error db-path (:message error)]))))))

(defn password-reset-confirmation-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (reset-password-clicked db-path))]
    [:form.pure-form.pure-form-stacked
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     "A password reset confirmation code was sent to your email address."
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form/field (conj db-path :code)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:input {:id "code"
                   :value value
                   :on-change on-change
                   :placeholder "Confirmation code"
                   :type "text"}]])]
      [form/field (conj db-path :new-password)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "new-password"} "New password:"]
          [:input {:id "new-password"
                   :value value
                   :on-change on-change
                   :placeholder "New password"
                   :type "password"}]])]
      [form/field (conj db-path :new-password-again)
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "new-password-again"} "New password again:"]
          [:input {:id "new-password-again"
                   :value value
                   :on-change on-change
                   :placeholder "New password again"
                   :type "password"}]])]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button.pure-button-primary {:type "submit"} "Reset password"]]]]))

(defn logout []
  ;; TODO: logout via API
  (remove-item local-storage local-storage-session-id-name)
  (remove-item local-storage local-storage-user-id-name)
  (remove-item local-storage local-storage-username-name)
  (state/invalidate-cache-all)
  (rf/dispatch [:remove user-db-path]))

(defn load-session-user-data []
  (when (not (data))
    (read-session-data)))

(defn login-modal [& [title]]
  (let [db-path [:login-form]]
    (modal/create (or title "Login") [login-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn sign-up-modal []
  (let [db-path [:sign-up-form]]
    (modal/create "Register" [sign-up-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn confirmation-modal []
  (let [db-path [:confirmation-form]]
    (modal/create "Register Confirmation" [confirmation-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn change-temporary-password-modal []
  (let [db-path [:change-temporary-password-form]]
    (modal/create "Change Temporary Password" [change-temporary-password-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn password-reset-confirmation-modal []
  (let [db-path [:password-reset-confirmation-form]]
    (modal/create "Reset Forgotten Password" [password-reset-confirmation-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))
