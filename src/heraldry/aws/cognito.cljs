(ns heraldry.aws.cognito
  (:require ["amazon-cognito-identity-js" :refer [CognitoUserPool
                                                  CognitoUser
                                                  AuthenticationDetails]]
            [heraldry.config :as config]))

(def user-pool
  (new CognitoUserPool (clj->js (select-keys (config/get :cognito-pool-config)
                                             [:UserPoolId :ClientId]))))

(defn login [username password & {:keys [on-success on-failure on-new-password-required on-confirmation-needed]}]
  (let [username     (or username "")
        password     (or password "")
        user         (new CognitoUser (clj->js {:Username username
                                                :Pool     user-pool}))
        auth-details (new AuthenticationDetails (clj->js {:Username username
                                                          :Password password}))]
    (.authenticateUser
     user
     auth-details
     (clj->js {:onSuccess           (fn [user]
                                      (on-success user))
               :onFailure           (fn [error]
                                      (let [error (js->clj error :keywordize-keys true)]
                                        (if (-> error :message (= "User is not confirmed."))
                                          (on-confirmation-needed user)
                                          (on-failure error))))
               :newPasswordRequired (fn [user-attributes _required-attributes]
                                      (let [user-attributes (-> user-attributes
                                                                (js->clj :keywordize-keys true)
                                                                (dissoc  :email_verified))]
                                        (on-new-password-required user user-attributes)))}))))

(defn sign-up [username password email & {:keys [on-success on-failure on-confirmation-needed]}]
  (let [username   (or username "")
        password   (or password "")
        attributes [{:Name  "email"
                     :Value email}]]
    (.signUp
     user-pool
     username
     password
     (clj->js attributes)
     nil
     (fn [err result]
       ;; data on success
       ;; {:user #object[CognitoUser [object Object]]
       ;;  :userConfirmed false
       ;;  :userSub <uuid>
       ;;  :codeDeliveryDetails {:AttributeName email,
       ;;                        :DeliveryMedium EMAIL,
       ;;                        :Destination <masked-email>}}
       (let [err    (js->clj err :keywordize-keys true)
             result (js->clj result :keywordize-keys true)]
         (cond
           err                            (on-failure err)
           (-> result :userConfirmed not) (on-confirmation-needed (:user result))
           :else                          (on-success (:user result))))))))

(defn confirm [user code & {:keys [on-success on-failure]}]
  (.confirmRegistration
   user
   code
   true
   (fn [err _result]
     (if err
       (on-failure (js->clj err :keywordize-keys true))
       (on-success user)))))

(defn resend-code [user & {:keys [on-success on-failure]}]
  (.resendConfirmationCode
   user
   (fn [err _result]
     (if err
       (on-failure (js->clj err :keywordize-keys true))
       (on-success nil)))))

(defn complete-new-password-challenge [user password user-attributes & {:keys [on-success on-failure]}]
  (.completeNewPasswordChallenge
   user
   password
   (clj->js user-attributes)
   (clj->js {:onSuccess (fn [result]
                          (on-success (js->clj result :keywordize-keys true)))
             :onFailure (fn [error]
                          (on-failure (js->clj error :keywordize-keys true)))})))

(defn forgot-password [username & {:keys [on-success on-failure]}]
  (let [user (new CognitoUser (clj->js {:Username username
                                        :Pool     user-pool}))]
    (.forgotPassword
     user
     (clj->js {:onSuccess (fn [_data]
                            (on-success user))
               :onFailure (fn [error]
                            (let [error (js->clj error :keywordize-keys true)]
                              (on-failure error)))}))))

(defn confirm-password [user code new-password & {:keys [on-success on-failure]}]
  (.confirmPassword
   user
   code
   new-password
   (clj->js {:onSuccess (fn [_data]
                          (on-success user))
             :onFailure (fn [error]
                          (let [error (js->clj error :keywordize-keys true)]
                            (on-failure error)))})))
