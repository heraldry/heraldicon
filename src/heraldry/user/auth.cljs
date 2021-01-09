(ns heraldry.user.auth
  (:require ["amazon-cognito-identity-js" :refer [CognitoUserPool
                                                  CognitoUser
                                                  AuthenticationDetails]]))

(def user-pool
  (new CognitoUserPool (clj->js {:UserPoolId "eu-central-1_2V31wcCCt"
                                 :ClientId "23nbkmboar365k4rfn2o6qrs0c"})))

(defn login [username password & {:keys [on-success on-failure on-new-password-required]}]
  (let [user (new CognitoUser (clj->js {:Username username
                                        :Pool user-pool}))
        auth-details (new AuthenticationDetails (clj->js {:Username username
                                                          :Password password}))]
    (.authenticateUser
     user
     auth-details
     (clj->js {:onSuccess (fn [result]
                            (on-success (js->clj result :keywordize-keys true)))
               :onFailure (fn [error]
                            (on-failure (js->clj error :keywordize-keys true)))
               :newPasswordRequired (fn [user-attributes _required-attributes]
                                      (let [user-attributes (dissoc (js->clj user-attributes :keywordize-keys true) :email_verified)]
                                        (on-new-password-required user user-attributes)))}))))
(defn sign-up [username password email & {:keys [on-success on-failure]}]
  (let [attributes [{:Name "email"
                     :Value email}]]
    (.signUp
     user-pool
     username password (clj->js attributes) nil
     (fn [err result]
       (if err
         (on-failure (js->clj err :keywordize-keys true))
         (on-success (js->clj result :keywordize-keys true)))))))

(defn confirm [user code & {:keys [on-success on-failure]}]
  (.confirmRegistration
   user
   code true
   (fn [err result]
     (if err
       (on-failure (js->clj err :keywordize-keys true))
       (on-success (js->clj result :keywordize-keys true))))))

(defn complete-new-password-challenge [user password user-attributes & {:keys [on-success on-failure]}]
  (.completeNewPasswordChallenge
   user
   password (clj->js user-attributes)
   (clj->js {:onSuccess (fn [result]
                          (on-success (js->clj result :keywordize-keys true)))
             :onFailure (fn [error]
                          (on-failure (js->clj error :keywordize-keys true)))})))
