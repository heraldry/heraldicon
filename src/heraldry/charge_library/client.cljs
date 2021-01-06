(ns heraldry.charge-library.client
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs.core.async :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.charge-library.api.request :as api-request]
            [heraldry.user.auth :as auth]
            [hickory.core :as hickory]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

;; subs

(rf/reg-sub
 :get
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-sub
 :get-form-data
 (fn [db [_ form-id]]
   (get-in db [:form-data form-id])))

(rf/reg-sub
 :get-form-error-message
 (fn [db [_ form-id]]
   (get-in db [:form-error-message form-id])))

(rf/reg-sub
 :get-form-error
 (fn [db [_ form-id key]]
   (get-in db [:form-errors form-id key])))

;; events

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {} db)))

(rf/reg-event-db
 :set
 (fn [db [_ path value]]
   (assoc-in db path value)))

(rf/reg-event-db
 :remove
 (fn [db [_ path]]
   (cond-> db
     (-> path count (= 1)) (dissoc (first path))
     (-> path count (> 1)) (update-in (drop-last path) dissoc (last path)))))

(rf/reg-event-db
 :set-form-data-key
 (fn [db [_ form-id key value]]
   (assoc-in db [:form-data form-id key] value)))

(rf/reg-event-db
 :set-form-error-message
 (fn [db [_ form-id message]]
   (assoc-in db [:form-error-message form-id] message)))

(rf/reg-event-db
 :set-form-error-key
 (fn [db [_ form-id key error]]
   (assoc-in db [:form-errors form-id key] error)))

(rf/reg-event-db
 :clear-form-errors
 (fn [db [_ form-id]]
   (update-in db [:form-errors] dissoc form-id)))

(rf/reg-event-db
 :clear-form
 (fn [db [_ form-id]]
   (-> db
       (update :form-data dissoc form-id)
       (update :form-error-message dissoc form-id)
       (update :form-errors dissoc form-id))))

;; functions


#_(defn strip-svg [data]
    (walk/postwalk (fn [x]
                     (cond->> x
                       (map? x) (into {}
                                      (filter (fn [[k _]]
                                                (or (-> k keyword? not)
                                                    (->> k name (s/split ":") count (= 1)))))))) data))

(defn optimize-svg [data]
  (go-catch
   (-> {:removeUnknownsAndDefaults false}
       clj->js
       getSvgoInstance
       (.optimize data)
       <p!
       (js->clj :keywordize-keys true)
       :data)))

;; views


(defn load-svg-file [form-id key data]
  (go
    (try
      (-> data
          optimize-svg
          <?
          hickory/parse-fragment
          first
          hickory/as-hiccup
          (as-> parsed
                (let [svg-data (-> parsed
                                   (assoc 0 :g)
                                   (assoc 1 {}))
                      width (-> parsed
                                (get-in [1 :width]))
                      height (-> parsed
                                 (get-in [1 :height]))]
                  (rf/dispatch [:set-form-data-key form-id key {:width width
                                                                :height height
                                                                :data svg-data}]))))
      (catch :default e
        (println "error:" e)))))

(defn preview [charge-data]
  [:div.preview
   (let [{:keys [width height data]} charge-data]
     (when data
       [:svg {:viewBox (str "0 0 " width " " height)
              :preserveAspectRatio "xMidYMid meet"}
        data]))])

(defn upload-file [event form-id key]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file form-id key raw-data))))
        (.readAsText reader file)))))

(defn form-field [form-id key function]
  (let [{value key} @(rf/subscribe [:get-form-data form-id])
        error @(rf/subscribe [:get-form-error form-id key])]
    (-> [:div.field {:class (when error "error")}]
        (cond->
         error (conj [:p.error-message error]))
        (conj (function :value value
                        :on-change #(let [new-value (-> % .-target .-value)]
                                      (rf/dispatch [:set-form-data-key form-id key new-value])))))))

(defn save-charge-form [form-id]
  (let [payload @(rf/subscribe [:get-form-data form-id])]
    (go
      (try
        (let [response (<! (api-request/call :save-charge payload))])
        (catch :default e
          (println "save-form error:" e))))))

(defn charge-form [form-id]
  (let [error-message @(rf/subscribe [:get-form-error-message form-id])
        charge-data @(rf/subscribe [:get-form-data form-id])]
    [:div
     [preview (:charge/data charge-data)]
     [:div.form
      (when error-message
        [:div.error-top
         [:p.error-message error-message]])
      [form-field form-id :charge/key
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "key"} "Charge Key"]
          [:input {:id "key"
                   :value value
                   :on-change on-change
                   :type "text"}]])]
      [form-field form-id :charge/name
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "name"} "Name"]
          [:input {:id "name"
                   :value value
                   :on-change on-change
                   :type "text"}]])]
      [form-field form-id :charge/attitude
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "attitude"} "Attitude"]
          [:input {:id "attitude"
                   :value value
                   :on-change on-change
                   :type "text"}]])]
      [form-field form-id :charge/data
       (fn [& _]
         [:<>
          [:label {:for "upload"
                   :style {:cursor "pointer"}} "Upload"]
          [:input {:type "file"
                   :accept "image/svg+xml"
                   :id "upload"
                   :on-change #(upload-file % form-id :charge/data)}]])]
      [:div.buttons
       [:button.save {:on-click #(save-charge-form form-id)} "Save"]]]]))

(defn login-clicked [form-id]
  (let [{:keys [username password]} @(rf/subscribe [:get-form-data form-id])]
    (rf/dispatch [:clear-form-errors])
    (auth/login username password
                :on-success (fn [result]
                              (println "login success" result)
                              (rf/dispatch [:clear-form form-id])
                              (rf/dispatch [:set [:user-data] {:jwt-token (-> result
                                                                              .getAccessToken
                                                                              .getJwtToken)
                                                               :logged-in? true}]))
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
      (auth/sign-up username password email
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
    (auth/confirm user code
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

(defn not-logged-in []
  (let [confirmation-needed? @(rf/subscribe [:get [:user-data :confirmation-needed?]])
        sign-up? @(rf/subscribe [:get [:sign-up?]])]
    (cond
      confirmation-needed? [confirmation-form]
      sign-up? [sign-up-form]
      :else [login-form])))

(defn app []
  (let [user-data @(rf/subscribe [:get [:user-data]])]
    (if (:logged-in? user-data)
      [charge-form :charge-form]
      [not-logged-in])))

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (r/render
   [app]
   (.getElementById js/document "app")))

(defn ^:export init []
  (start))
