(ns heraldry.frontend.user
  (:require [cljs.core.async :refer [<! go]]
            [heraldry.api.request :as api-request]
            [heraldry.aws.cognito :as cognito]
            [heraldry.frontend.modal :as modal]
            [hodgepodge.core :refer [local-storage get-item remove-item set-item]]
            [re-frame.core :as rf]))

(def db-path [:user-data])

(def local-storage-session-id-name
  "cl-session-id")

(def local-storage-user-id-name
  "cl-user-id")

(def local-storage-username-name
  "cl-username")

(declare login-modal)
(declare confirmation-modal)

(defn form-field [form-id key function]
  (let [{value key} @(rf/subscribe [:get-form-data form-id])
        error       @(rf/subscribe [:get-form-error form-id key])]
    (-> [:div {:class (when error "error")}]
        (cond->
            error (conj [:div.error-message error]))
        (conj (function :value value
                        :on-change #(let [new-value (-> % .-target .-value)]
                                      (rf/dispatch [:set-form-data-key form-id key new-value])))))))

(defn read-session-data []
  (let [session-id (get-item local-storage local-storage-session-id-name)
        user-id    (get-item local-storage local-storage-user-id-name)
        username   (get-item local-storage local-storage-username-name)]
    (rf/dispatch-sync [:set db-path
                       (if (and session-id username user-id)
                         {:username   username
                          :session-id session-id
                          :user-id    user-id
                          :logged-in? true}
                         nil)])))

(defn complete-login [form-id jwt-token]
  (go
    (-> (api-request/call :login {:jwt-token jwt-token} nil)
        <!
        (as-> response
            (if-let [error (:error response)]
              (do
                (println "error:" error)
                (rf/dispatch [:set-form-error-message form-id error]))
              (let [{:keys [session-id username user-id]} response]
                (set-item local-storage local-storage-session-id-name session-id)
                (set-item local-storage local-storage-username-name username)
                (set-item local-storage local-storage-user-id-name user-id)
                (read-session-data)
                (rf/dispatch [:clear-form form-id])
                (modal/clear)))))))

(defn login-clicked [form-id]
  (let [{:keys [username password]} @(rf/subscribe [:get-form-data form-id])]
    (rf/dispatch [:clear-form-errors])
    (cognito/login username
                   password
                   :on-success (fn [user]
                                 (println "login success" user)
                                 (complete-login form-id (-> user
                                                             .getAccessToken
                                                             .getJwtToken)))
                   :on-confirmation-needed (fn [user]
                                             (rf/dispatch [:clear-form form-id])
                                             (rf/dispatch [:set db-path
                                                           {:user user}])
                                             (confirmation-modal))
                   :on-failure (fn [error]
                                 (println "login failure" error)
                                 (rf/dispatch [:set-form-error-message form-id (:message error)])))))

(defn login-form []
  (let [form-id       :login-form
        error-message @(rf/subscribe [:get-form-error-message form-id])
        on-submit     (fn [event]
                        (.preventDefault event)
                        (.stopPropagation event)
                        (login-clicked form-id))]
    [:form.pure-form.pure-form-stacked
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit    on-submit}
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form-field form-id :username
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:input {:id          "username"
                   :value       value
                   :on-change   on-change
                   :placeholder "Username"
                   :type        "text"}]])]
      [form-field form-id :password
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:input {:id          "password"
                   :value       value
                   :on-change   on-change
                   :placeholder "Password"
                   :type        "password"}]])]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button
        {:style    {:margin-right "5px"}
         :type     "reset"
         :on-click #(do
                      (rf/dispatch [:clear-form form-id])
                      (modal/clear))}
        "Cancel"]
       [:button.pure-button.pure-button-primary {:type "submit"}
        "Login"]]]]))

(defn sign-up-clicked [form-id]
  (let [{:keys [username email password password-again]} @(rf/subscribe [:get-form-data form-id])]
    (rf/dispatch [:clear-form-errors])
    (if (not= password password-again)
      (rf/dispatch [:set-form-error-key form-id :password-again "Passwords don't match."])
      (cognito/sign-up username password email
                       :on-success (fn [user]
                                     (println "sign-up success" user)
                                     (rf/dispatch [:clear-form form-id])
                                     (login-modal "Registration completed"))
                       :on-confirmation-needed (fn [user]
                                                 (println "sign-up success, confirmation needed" user)
                                                 (rf/dispatch [:clear-form form-id])
                                                 (rf/dispatch [:set db-path {:user user}])
                                                 (confirmation-modal))
                       :on-failure (fn [error]
                                     (println "sign-up failure" error)
                                     (rf/dispatch [:set-form-error-message form-id (:message error)]))))))

(defn sign-up-form []
  (let [form-id       :sign-up-form
        error-message @(rf/subscribe [:get-form-error-message form-id])
        on-submit     (fn [event]
                        (.preventDefault event)
                        (.stopPropagation event)
                        (sign-up-clicked form-id))]
    [:form.pure-form.pure-form-aligned
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit    on-submit}
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form-field form-id :username
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "username"} "Username"]
          [:input {:id          "username"
                   :value       value
                   :on-change   on-change
                   :placeholder "Username"
                   :type        "text"}]])]
      [form-field form-id :email
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "email"} "Email"]
          [:input {:id          "email"
                   :value       value
                   :on-change   on-change
                   :placeholder "Email"
                   :type        "text"}]])]
      [form-field form-id :password
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "password"} "Password:"]
          [:input {:id          "password"
                   :value       value
                   :on-change   on-change
                   :placeholder "Password"
                   :type        "password"}]])]
      [form-field form-id :password-again
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:label {:for "password-again"} "Password again:"]
          [:input {:id          "password-again"
                   :value       value
                   :on-change   on-change
                   :placeholder "Password again"
                   :type        "password"}]])]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button
        {:style    {:margin-right "5px"}
         :type     "reset"
         :on-click #(do
                      (rf/dispatch [:clear-form form-id])
                      (modal/clear))}
        "Cancel"]
       [:button.pure-button.pure-button-primary {:type "submit"}
        "Register"]]]]))

(defn confirm-clicked [form-id]
  (let [{:keys [code]} @(rf/subscribe [:get-form-data form-id])
        user-data      @(rf/subscribe [:get db-path])
        user           (:user user-data)]
    (rf/dispatch [:clear-form-errors form-id])
    (cognito/confirm user code
                     :on-success (fn [user]
                                   (println "confirm success" user)
                                   (rf/dispatch [:clear-form form-id])
                                   (login-modal "Registration completed"))
                     :on-failure (fn [error]
                                   (println "confirm error" error)
                                   (rf/dispatch [:set-form-error-message form-id (:message error)])))))

(defn confirmation-form []
  (let [form-id       :confirmation-form
        error-message @(rf/subscribe [:get-form-error-message form-id])
        on-submit     (fn [event]
                        (.preventDefault event)
                        (.stopPropagation event)
                        (confirm-clicked form-id))]
    [:form.pure-form.pure-form-stacked
     {:on-key-press (fn [event]
                      (when (-> event .-code (= "Enter"))
                        (on-submit event)))
      :on-submit    on-submit}
     "A confirmation code was sent to your email address."
     (when error-message
       [:div.error-message error-message])
     [:fieldset
      [form-field form-id :code
       (fn [& {:keys [value on-change]}]
         [:div.pure-control-group
          [:input {:id          "code"
                   :value       value
                   :on-change   on-change
                   :placeholder "Confirmation code"
                   :type        "text"}]])]
      [:div.pure-control-group {:style {:text-align "right"
                                        :margin-top "10px"}}
       [:button.pure-button.pure-button-primary {:type "submit"} "Confirm"]]]]))

(defn logout []
  ;; TODO: logout via API
  (remove-item local-storage local-storage-session-id-name)
  (remove-item local-storage local-storage-user-id-name)
  (remove-item local-storage local-storage-username-name)
  (rf/dispatch [:remove db-path]))

(defn load-session-user-data []
  (when (not @(rf/subscribe [:get db-path]))
    (read-session-data)))

(defn data []
  @(rf/subscribe [:get db-path]))

(defn login-modal [& [title]]
  (modal/create (or title "Login") [login-form]))

(defn sign-up-modal []
  (modal/create "Register" [sign-up-form]))

(defn confirmation-modal []
  (modal/create "Register Confirmation" [confirmation-form]))
