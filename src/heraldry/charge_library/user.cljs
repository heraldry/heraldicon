(ns heraldry.charge-library.user
  (:require [cljs.core.async :refer [<! go]]
            [heraldry.charge-library.api.request :as api-request]
            [heraldry.aws.cognito :as cognito]
            [hodgepodge.core :refer [local-storage get-item remove-item set-item]]
            [re-frame.core :as rf]))

(def local-storage-session-id-name
  "cl-session-id")

(def local-storage-user-id-name
  "cl-user-id")

(def local-storage-username-name
  "cl-username")

(defn form-field [form-id key function]
  (let [{value key} @(rf/subscribe [:get-form-data form-id])
        error @(rf/subscribe [:get-form-error form-id key])]
    (-> [:div.field {:class (when error "error")}]
        (cond->
         error (conj [:p.error-message error]))
        (conj (function :value value
                        :on-change #(let [new-value (-> % .-target .-value)]
                                      (rf/dispatch [:set-form-data-key form-id key new-value])))))))

(defn login-clicked [form-id]
  (let [{:keys [username password]} @(rf/subscribe [:get-form-data form-id])]
    (rf/dispatch [:clear-form-errors])
    (cognito/login username password
                   :on-success (fn [result]
                                 (println "login success" result)
                                 (let [jwt-token (-> result
                                                     .getAccessToken
                                                     .getJwtToken)]
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
                                                   (rf/dispatch [:set [:user-data] (assoc response :logged-in? true)])
                                                   (rf/dispatch [:clear-form form-id]))))))))

                   :on-failure (fn [error]
                                 (println "login failure" error)
                                 (rf/dispatch [:set-form-error-message form-id (:message error)])))))

(defn login-form []
  (let [form-id :login-form
        error-message @(rf/subscribe [:get-form-error-message form-id])]
    [:div.form.login
     [:h4 {:style {:margin 0
                   :margin-bottom "0.5em"}} "Login"]
     (when error-message
       [:div.error-top
        [:p.error-message error-message]])
     [form-field form-id :username
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "username"} "Username"]
         [:input {:id "username"
                  :value value
                  :on-change on-change
                  :type "text"}]])]
     [form-field form-id :password
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "password"} "Password:"]
         [:input {:id "password"
                  :value value
                  :on-change on-change
                  :type "password"}]])]
     [:div.buttons
      [:button.login {:on-click #(login-clicked form-id)} "Login"]]
     [:span {:style {:text-align "center"}}
      [:a {:on-click #(rf/dispatch [:set [:sign-up?] true])} "Sign up"]]]))

(defn sign-up-clicked [form-id]
  (let [{:keys [username email password password-again]} @(rf/subscribe [:get-form-data form-id])]
    (rf/dispatch [:clear-form-errors])
    (if (not= password password-again)
      (rf/dispatch [:set-form-error-key form-id :password-again "Passwords don't match."])
      (cognito/sign-up username password email
                       :on-success (fn [result]
                                     ;; data:
                                     ;; {:user #object[CognitoUser [object Object]]
                                     ;; :userConfirmed false
                                     ;; :userSub <uuid>
                                     ;; :codeDeliveryDetails {:AttributeName email,
                                     ;; :DeliveryMedium EMAIL,
                                     ;; :Destination <masked-email>}}
                                     (println "sign-up success" result)
                                     (rf/dispatch [:clear-form form-id])
                                     (rf/dispatch [:set [:user-data] {:user (:user result)
                                                                      :confirmation-needed? true
                                                                      :confirmation-email (-> result
                                                                                              :codeDeliveryDetails
                                                                                              :Destination)}]))
                       :on-failure (fn [error]
                                     (println "sign-up failure" error)
                                     (rf/dispatch [:set-form-error-message form-id (:message error)]))))))

(defn sign-up-form []
  (let [form-id :sign-up-form
        error-message @(rf/subscribe [:get-form-error-message form-id])]
    [:div.form.login
     [:h4 {:style {:margin 0
                   :margin-bottom "0.5em"}} "Sign-up"]
     (when error-message
       [:div.error-top
        [:p.error-message error-message]])
     [form-field form-id :username
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "username"} "Username"]
         [:input {:id "username"
                  :value value
                  :on-change on-change
                  :type "text"}]])]
     [form-field form-id :email
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "email"} "Email"]
         [:input {:id "email"
                  :value value
                  :on-change on-change
                  :type "text"}]])]
     [form-field form-id :password
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "password"} "Password:"]
         [:input {:id "password"
                  :value value
                  :on-change on-change
                  :type "password"}]])]
     [form-field form-id :password-again
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "password-again"} "Password again:"]
         [:input {:id "password-again"
                  :value value
                  :on-change on-change
                  :type "password"}]])]
     [:div.buttons
      [:button.sign-up {:on-click #(sign-up-clicked form-id)} "Sign-up"]]
     [:span {:style {:text-align "center"}}
      [:a {:on-click #(rf/dispatch [:set [:sign-up?] false])} "Back to login-clicked"]]]))

(defn confirm-clicked [form-id]
  (let [{:keys [code]} @(rf/subscribe [:get-form-data form-id])
        user-data @(rf/subscribe [:get [:user-data]])
        user (:user user-data)]
    (rf/dispatch [:clear-form-errors form-id])
    (cognito/confirm user code
                     :on-success (fn [result]
                                   (println "confirm success" result)
                                   (rf/dispatch [:clear-form form-id])
                                   (rf/dispatch [:set [:sign-up?] false])
                                   (rf/dispatch [:set [:user-data] {:user user
                                                                    :logged-in? true}]))
                     :on-failure (fn [error]
                                   (println "confirm error" error)
                                   (rf/dispatch [:set-form-error-message form-id (:message error)])))))

(defn confirmation-form []
  (let [form-id :confirmation-form
        user-data @(rf/subscribe [:get [:user-data]])
        error-message @(rf/subscribe [:get-form-error-message form-id])]
    [:div.form.login
     [:h4 {:style {:margin 0
                   :margin-bottom "0.5em"}} "Confirmation"]
     "A confirmation code was sent to your email address: " (:confirmation-email user-data)
     (when error-message
       [:div.error-top
        [:p.error-message error-message]])
     [form-field form-id :code
      (fn [& {:keys [value on-change]}]
        [:<>
         [:label {:for "code"} "Confirmation code"]
         [:input {:id "code"
                  :value value
                  :on-change on-change
                  :type "text"}]])]

     [:div.buttons
      [:button.confirm {:on-click #(confirm-clicked form-id)} "Confirm"]]]))

(defn logout []
  ;; TODO: logout via API
  (remove-item local-storage local-storage-session-id-name)
  (remove-item local-storage local-storage-user-id-name)
  (remove-item local-storage local-storage-username-name)
  (rf/dispatch [:remove [:user-data]]))

(defn load-session-user-data []
  (let [session-id (get-item local-storage local-storage-session-id-name)
        user-id (get-item local-storage local-storage-user-id-name)
        username (get-item local-storage local-storage-username-name)]
    (if (and session-id username user-id)
      {:username username
       :session-id session-id
       :user-id user-id
       :logged-in? true}
      nil)))
