(ns heraldry.frontend.charge-library
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs.core.async :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user :refer [form-field]]
            [hickory.core :as hickory]
            [re-frame.core :as rf]))

;; subs


(rf/reg-sub
 :api-fetch-charges-by-user
 (fn [db [_ user-id]]
   (let [data (get-in db [:charges-by-user user-id])
         user-data (user/data)]
     (cond
       (= data :loading) nil
       data data
       :else (do
               (rf/dispatch-sync [:set [:charges-by-user user-id] :loading])
               (go
                 (-> (api-request/call :list-charges {:user-id user-id} user-data)
                     <!
                     (as-> response
                           (let [error (:error response)]
                             (if error
                               (println ":fetch-charges-by-user error:" error)
                               (rf/dispatch-sync [:set [:charges-by-user user-id] (:charges response)]))))))
               nil)))))

(rf/reg-sub
 :api-fetch-charge-by-id-to-form
 (fn [db [_ charge-id form-id]]
   (let [data (get-in db [:charge-by-id charge-id])
         user-data (user/data)]
     (cond
       (= data :loading) nil
       data data
       :else (do
               (rf/dispatch-sync [:set [:charge-by-id charge-id] :loading])
               (go
                 (-> (api-request/call :fetch-charge {:id charge-id} user-data)
                     <!
                     (as-> response
                           (if-let [error (:error response)]
                             (println ":fetch-charge-by-id error:" error)
                             (do
                               (rf/dispatch-sync [:set-form-data form-id response])
                               (rf/dispatch-sync [:set [:charge-by-id charge-id]
                                                  response]))))))
               nil)))))

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
                (let [edn-data (-> parsed
                                   (assoc 0 :g)
                                   (assoc 1 {}))
                      width (-> parsed
                                (get-in [1 :width])
                                js/parseFloat)
                      height (-> parsed
                                 (get-in [1 :height])
                                 js/parseFloat)]
                  (rf/dispatch [:set-form-data-key form-id :width width])
                  (rf/dispatch [:set-form-data-key form-id :height height])
                  (rf/dispatch [:set-form-data-key form-id key {:edn-data edn-data
                                                                :svg-data data}]))))
      (catch :default e
        (println "error:" e)))))

(defn preview [width height charge-data]
  (let [{:keys [edn-data]} charge-data]
    (when edn-data
      [:svg {:viewBox (str "0 0 " width " " height)
             :preserveAspectRatio "xMidYMid meet"}
       edn-data])))

(defn upload-file [event form-id key]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file form-id key raw-data))))
        (.readAsText reader file)))))

(defn save-charge-clicked [form-id]
  (let [payload @(rf/subscribe [:get-form-data form-id])
        user-data (user/data)]
    (go
      (try
        (let [response (<! (api-request/call :save-charge payload user-data))
              error (:error response)
              charge-id (-> response :charge-id)]
          (println "save charge response" response)
          (when-not error
            (rf/dispatch [:set-form-data-key form-id :id charge-id])))
        (catch :default e
          (println "save-form error:" e))))))

(defn charge-form [charge-id]
  (let [form-id :charge-form
        error-message @(rf/subscribe [:get-form-error-message form-id])
        {:keys [width height data]} @(rf/subscribe [:get-form-data form-id])
        _charge-data @(rf/subscribe [:api-fetch-charge-by-id-to-form charge-id form-id])]
    [:div.pure-g
     [:div.pure-u-1-2
      [preview width height data]]
     [:div.pure-u-1-2
      [:form.pure-form.pure-form-aligned {:style {:display "inline-block"}}
       (when error-message
         [:div.error-message error-message])
       [:fieldset
        [form-field form-id :key
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for "key"} "Charge Key"]
            [:input {:id "key"
                     :value value
                     :on-change on-change
                     :type "text"}]])]
        [form-field form-id :name
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for "name"} "Name"]
            [:input {:id "name"
                     :value value
                     :on-change on-change
                     :type "text"}]])]
        [form-field form-id :attitude
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for "attitude"} "Attitude"]
            [:input {:id "attitude"
                     :value value
                     :on-change on-change
                     :type "text"}]])]
        [form-field form-id :data
         (fn [& _]
           [:div.pure-control-group
            [:label {:for "upload"} "Upload"]
            [:input {:type "file"
                     :accept "image/svg+xml"
                     :id "upload"
                     :on-change #(upload-file % form-id :data)}]])]]
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [:button.pure-button.pure-button-primary {:on-click #(save-charge-clicked form-id)} "Save"]]]]]))

(defn list-charges-for-user []
  (let [user-data (user/data)
        charge-list @(rf/subscribe [:api-fetch-charges-by-user (:user-id user-data)])]
    [:div
     [:h4 "My charges"]
     (if charge-list
       [:ul.charge-list
        (doall
         (for [charge charge-list]
           ^{:key (:id charge)}
           [:li.charge
            (let [href (str (state/path) "#" (:id charge))]
              [:a {:href href
                   :on-click #(do
                                (.preventDefault %)
                                (state/goto href))}
               (:name charge) " "
               [:i.far.fa-edit]])]))]
       [:<>])]))

(defn logged-in []
  (let [charge-id (state/path-extra)]
    (if charge-id
      [charge-form charge-id]
      [list-charges-for-user])))

(defn not-logged-in []
  [:div "You need to be logged in."])

(defn main []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [logged-in]
      [not-logged-in])))
