(ns heraldry.frontend.user
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.aws.cognito :as cognito]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.language :refer [tr]]
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

(defn text-field [db-path function]
  (let [value @(rf/subscribe [:get db-path])
        error @(rf/subscribe [:get-form-error db-path])]
    [:div {:class (when error "error")}
     (when error
       [:div.error-message [tr error]])
     (function :value value
               :on-change #(let [new-value (-> % .-target .-value)]
                             (rf/dispatch-sync [:set db-path new-value])))]))
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
        (state/invalidate-cache-all-but-new)
        (modal/clear)
        (modal/stop-loading))
      (catch :default e
        (log/error "complete login error:" e)
        (rf/dispatch [:set-form-error db-path (:message e)])
        (modal/stop-loading)))))

(defn login-clicked [db-path]
  (modal/start-loading)
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
                                             (confirmation-modal)
                                             (modal/stop-loading))
                   :on-failure (fn [error]
                                 (log/error "login error:" error)
                                 (rf/dispatch [:set-form-error db-path (:message error)])
                                 (modal/stop-loading))
                   :on-new-password-required (fn [user user-attributes]
                                               (rf/dispatch [:clear-form db-path])
                                               (rf/dispatch [:set (conj user-db-path :user) user])
                                               (rf/dispatch [:set (conj user-db-path :user-attributes) user-attributes])
                                               (change-temporary-password-modal)
                                               (modal/stop-loading)))))
(defn forgotten-password-clicked [db-path]
  (let [{:keys [username]} @(rf/subscribe [:get db-path])]
    (rf/dispatch-sync [:clear-form-errors db-path])
    (if (-> username
            count
            zero?)
      (rf/dispatch [:set-form-error (conj db-path :username) :string.user.message/username-required-for-password-reset])
      (do
        (modal/start-loading)
        (cognito/forgot-password
         username
         :on-success (fn [user]
                       (rf/dispatch [:clear-form db-path])
                       (rf/dispatch [:set (conj user-db-path :user) user])
                       (password-reset-confirmation-modal)
                       (modal/stop-loading))
         :on-failure (fn [error]
                       (log/error "password reset initiation error:" error)
                       (rf/dispatch [:set-form-error db-path (:message error)])
                       (modal/stop-loading)))))))

(defn login-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (login-clicked db-path))]
    [:form.modal-form
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     (when error-message
       [:div.error-message [tr error-message]])
     [text-field (conj db-path :username)
      (fn [& {:keys [value on-change]}]
        [:div
         [:input {:id "username"
                  :name "username"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/username)
                  :type "text"}]])]
     [text-field (conj db-path :password)
      (fn [& {:keys [value on-change]}]
        [:div
         [:input {:id "password"
                  :name "password"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/password)
                  :type "password"}]])]
     [:a
      {:style {:margin-right "5px"}
       :href "#"
       :on-click (fn [event]
                   (.preventDefault event)
                   (.stopPropagation event)
                   (forgotten-password-clicked db-path))}
      [tr :string.user/forgotten-password]]
     [:div {:style {:text-align "right"
                    :margin-top "10px"}}
      [:button.button
       {:style {:margin-right "5px"}
        :type "reset"
        :on-click #(do
                     (rf/dispatch [:clear-form db-path])
                     (modal/clear))}
       [tr :string.button/cancel]]
      [:button.button.primary {:type "submit"}
       [tr :string.menu/login]]]]))

(defn sign-up-clicked [db-path]
  (let [{:keys [username email password password-again]} @(rf/subscribe [:get db-path])]
    (rf/dispatch-sync [:clear-form-errors])
    (if (not= password password-again)
      (rf/dispatch [:set-form-error (conj db-path :password-again) :string.user.message/passwords-do-not-match])
      (do
        (modal/start-loading)
        (cognito/sign-up username password email
                         :on-success (fn [_user]
                                       (rf/dispatch [:clear-form db-path])
                                       (login-modal :string.user.message/registration-completed)
                                       (modal/stop-loading))
                         :on-confirmation-needed (fn [user]
                                                   (rf/dispatch [:clear-form db-path])
                                                   (rf/dispatch [:set (conj user-db-path :user) user])
                                                   (confirmation-modal)
                                                   (modal/stop-loading))
                         :on-failure (fn [error]
                                       (log/error "sign-up error" error)
                                       (rf/dispatch [:set-form-error db-path (:message error)])
                                       (modal/stop-loading)))))))

(defn sign-up-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (sign-up-clicked db-path))]
    [:form.modal-form
     {:autoComplete "off"
      :on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     (when error-message
       [:div.error-message [tr error-message]])
     [text-field (conj db-path :username)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "username"} [tr :string.user/username]]
         [:input {:id "username"
                  :name "username"
                  :autoComplete "off"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/username)
                  :type "text"}]])]
     [text-field (conj db-path :email)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "email"} [tr :string.user/email]]
         [:input {:id "email"
                  :name "email"
                  :autoComplete "off"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/email)
                  :type "text"}]])]
     [text-field (conj db-path :password)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "password"} [tr :string.user/password]]
         [:input {:id "password"
                  :name "password"
                  :autoComplete "off"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/password)
                  :type "password"}]])]
     [text-field (conj db-path :password-again)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "password-again"} [tr :string.user/password-again]]
         [:input {:id "password-again"
                  :name "password-again"
                  :autoComplete "off"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/password-again)
                  :type "password"}]])]
     [:div {:style {:text-align "right"
                    :margin-top "10px"}}
      [:button.button
       {:style {:margin-right "5px"}
        :type "reset"
        :on-click #(do
                     (rf/dispatch [:clear-form db-path])
                     (modal/clear))}
       [tr :string.button/cancel]]
      [:button.button.primary {:type "submit"}
       [tr :string.menu/register]]]]))

(defn confirm-clicked [db-path]
  (let [{:keys [code]} @(rf/subscribe [:get db-path])
        user-data (data)
        user (:user user-data)]
    (rf/dispatch-sync [:clear-form-errors db-path])
    (modal/start-loading)
    (cognito/confirm user code
                     :on-success (fn [_user]
                                   (rf/dispatch [:clear-form db-path])
                                   (login-modal :string.user.message/registration-completed)
                                   (modal/stop-loading))
                     :on-failure (fn [error]
                                   (log/error "confirmation error:" error)
                                   (rf/dispatch [:set-form-error db-path (:message error)])
                                   (modal/stop-loading)))))

(defn resend-code-clicked [db-path]
  (let [user-data (data)
        user (:user user-data)]
    (modal/start-loading)
    (cognito/resend-code user
                         :on-success (fn []
                                       (js/alert (tr :string.user.message/new-confirmation-code-sent))
                                       (modal/stop-loading))
                         :on-failure (fn [error]
                                       (log/error "resend code error:" error)
                                       (rf/dispatch [:set-form-error db-path (:message error)])
                                       (modal/stop-loading)))))

(defn confirmation-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (confirm-clicked db-path))]
    [:form.modal-form
     {:autoComplete "off"
      :on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     [:p [tr :string.user.message/confirmation-code-sent]]
     (when error-message
       [:div.error-message [tr error-message]])
     [text-field (conj db-path :code)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "code"} [tr :string.user/confirmation-code]]
         [:input {:id "code"
                  :name "code"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/confirmation-code)
                  :type "text"}]])]
     [:div {:style {:text-align "right"
                    :margin-top "10px"}}
      [:button.button
       {:style {:margin-right "5px"}
        :type "button"
        :on-click #(resend-code-clicked db-path)}
       [tr :string.user.button/resend-code]]
      [:button.button.primary {:type "submit"} [tr :string.user.button/confirm]]]]))

(defn change-temporary-password-clicked [db-path]
  (let [user-data (data)
        user (:user user-data)
        user-attributes (:user-attributes user-data)
        {:keys [new-password
                new-password-again]} @(rf/subscribe [:get db-path])]
    (rf/dispatch-sync [:clear-form-errors])
    (if (not= new-password new-password-again)
      (rf/dispatch [:set-form-error (conj db-path :new-password-again) :string.user.message/passwords-do-not-match])
      (do
        (modal/start-loading)
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
                       (rf/dispatch [:set-form-error db-path (:message error)])
                       (modal/stop-loading)))))))

(defn change-temporary-password-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (change-temporary-password-clicked db-path))]
    [:form.modal-form
     {:autoComplete "off"
      :on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     (when error-message
       [:div.error-message [tr error-message]])
     [text-field (conj db-path :new-password)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "new-password"} [tr :string.user/new-password]]
         [:input {:id "new-password"
                  :name "new-password"
                  :value value
                  :on-change on-change
                  :autoComplete "off"
                  :placeholder (tr :string.user/new-password)
                  :type "password"}]])]
     [text-field (conj db-path :new-password-again)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "new-password-again"} [tr :string.user/new-password-again]]
         [:input {:id "new-password-again"
                  :name "new-password-again"
                  :value value
                  :autoComplete "off"
                  :on-change on-change
                  :placeholder (tr :string.user/new-password-again)
                  :type "password"}]])]
     [:div {:style {:text-align "right"
                    :margin-top "10px"}}
      [:button.button
       {:style {:margin-right "5px"}
        :type "reset"
        :on-click #(do
                     (rf/dispatch [:clear-form db-path])
                     (modal/clear))}
       [tr :string.button/cancel]]
      [:button.button.primary {:type "submit"}
       [tr :string.user.button/change]]]]))

(defn reset-password-clicked [db-path]
  (let [{:keys [code
                new-password
                new-password-again]} @(rf/subscribe [:get db-path])
        user-data (data)
        user (:user user-data)]
    (rf/dispatch-sync [:clear-form-errors db-path])
    (if (not= new-password new-password-again)
      (rf/dispatch [:set-form-error (conj db-path :new-password-again) :string.user.message/passwords-do-not-match])
      (do
        (modal/start-loading)
        (cognito/confirm-password
         user
         code
         new-password
         :on-success (fn [_user]
                       (rf/dispatch [:clear-form db-path])
                       (login-modal :string.user.message/password-reset-completed)
                       (modal/stop-loading))
         :on-failure (fn [error]
                       (log/error "password reset error:" error)
                       (rf/dispatch [:set-form-error db-path (:message error)])
                       (modal/stop-loading)))))))

(defn password-reset-confirmation-form [db-path]
  (let [error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (reset-password-clicked db-path))]
    [:form.modal-form
     {:autoComplete "off"
      :on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit on-submit}
     [:p [tr :string.user.message/password-reset-confirmation-code-sent]]
     (when error-message
       [:div.error-message [tr error-message]])
     [text-field (conj db-path :code)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "code"} [tr :string.user/confirmation-code]]
         [:input {:id "code"
                  :name "code"
                  :autoComplete "off"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/confirmation-code)
                  :type "text"}]])]
     [text-field (conj db-path :new-password)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "new-password"} [tr :string.user/new-password]]
         [:input {:id "new-password"
                  :name "new-password"
                  :autoComplete "off"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/new-password)
                  :type "password"}]])]
     [text-field (conj db-path :new-password-again)
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "new-password-again"} [tr :string.user/new-password-again]]
         [:input {:id "new-password-again"
                  :name "new-password-again"
                  :autoComplete "off"
                  :value value
                  :on-change on-change
                  :placeholder (tr :string.user/new-password-again)
                  :type "password"}]])]
     [:div {:style {:text-align "right"
                    :margin-top "10px"}}
      [:button.button.primary {:type "submit"} [tr :string.user.button/reset-password]]]]))

(defn logout []
  ;; TODO: logout via API
  (remove-item local-storage local-storage-session-id-name)
  (remove-item local-storage local-storage-user-id-name)
  (remove-item local-storage local-storage-username-name)
  (state/invalidate-cache-all-but-new)
  (rf/dispatch [:remove user-db-path]))

(defn load-session-user-data []
  (when (not (data))
    (read-session-data)))

(defn login-modal [& [title]]
  (let [db-path [:login-form]]
    (modal/create (or title :string.menu/login) [login-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn sign-up-modal []
  (let [db-path [:sign-up-form]]
    (modal/create :string.menu/register [sign-up-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn confirmation-modal []
  (let [db-path [:confirmation-form]]
    (modal/create :string.user/register-confirmation [confirmation-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn change-temporary-password-modal []
  (let [db-path [:change-temporary-password-form]]
    (modal/create :string.user/change-temporary-password [change-temporary-password-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))

(defn password-reset-confirmation-modal []
  (let [db-path [:password-reset-confirmation-form]]
    (modal/create :string.user/reset-forgotten-password [password-reset-confirmation-form db-path]
                  :on-cancel #(rf/dispatch [:clear-form db-path]))))
