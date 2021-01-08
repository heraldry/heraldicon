(ns heraldry.charge-library.client
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs.core.async :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.charge-library.api.request :as api-request]
            [heraldry.charge-library.user :as user :refer [form-field]]
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
                    width    (-> parsed
                                 (get-in [1 :width]))
                    height   (-> parsed
                                 (get-in [1 :height]))]
                (rf/dispatch [:set-form-data-key form-id key {:width  width
                                                              :height height
                                                              :data   svg-data}]))))
      (catch :default e
        (println "error:" e)))))

(defn preview [charge-data]
  [:div.preview
   (let [{:keys [width height data]} charge-data]
     (when data
       [:svg {:viewBox             (str "0 0 " width " " height)
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

(defn save-charge-form [form-id]
  (let [payload   @(rf/subscribe [:get-form-data form-id])
        user-data @(rf/subscribe [:get [:user-data]])]
    (go
      (try
        (let [response  (<! (api-request/call :save-charge payload user-data))
              error     (:error response)
              charge-id (-> response :charge-id)]
          (println "save charge response" response)
          (when-not error
            (rf/dispatch [:set-form-data-key form-id :id charge-id])))
        (catch :default e
          (println "save-form error:" e))))))

(defn charge-form [form-id]
  (let [error-message @(rf/subscribe [:get-form-error-message form-id])
        charge-data   @(rf/subscribe [:get-form-data form-id])]
    [:div
     [preview (:data charge-data)]
     [:div.form
      (when error-message
        [:div.error-top
         [:p.error-message error-message]])
      [form-field form-id :key
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "key"} "Charge Key"]
          [:input {:id        "key"
                   :value     value
                   :on-change on-change
                   :type      "text"}]])]
      [form-field form-id :name
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "name"} "Name"]
          [:input {:id        "name"
                   :value     value
                   :on-change on-change
                   :type      "text"}]])]
      [form-field form-id :attitude
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "attitude"} "Attitude"]
          [:input {:id        "attitude"
                   :value     value
                   :on-change on-change
                   :type      "text"}]])]
      [form-field form-id :data
       (fn [& _]
         [:<>
          [:label {:for   "upload"
                   :style {:cursor "pointer"}} "Upload"]
          [:input {:type      "file"
                   :accept    "image/svg+xml"
                   :id        "upload"
                   :on-change #(upload-file % form-id :data)}]])]
      [:div.buttons
       [:button.save {:on-click #(save-charge-form form-id)} "Save"]]
      [:div.buttons
       [:button.logout {:on-click #(user/logout)} "Logout"]]]]))

(defn not-logged-in []
  (let [confirmation-needed? @(rf/subscribe [:get [:user-data :confirmation-needed?]])
        sign-up?             @(rf/subscribe [:get [:sign-up?]])]
    (cond
      confirmation-needed? [user/confirmation-form]
      sign-up?             [user/sign-up-form]
      :else                [user/login-form])))

(defn app []
  (let [user-data @(rf/subscribe [:get [:user-data]])]
    (if (:logged-in? user-data)
      [charge-form :charge-form]
      [not-logged-in])))

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:set [:user-data] (user/load-session-user-data)])
  (r/render
   [app]
   (.getElementById js/document "app")))

(defn ^:export init []
  (start))
