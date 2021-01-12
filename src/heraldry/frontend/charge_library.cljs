(ns heraldry.frontend.charge-library
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs-http.client :as http]
            [cljs.core.async :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.reader :as reader]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.form :as form]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [hickory.core :as hickory]
            [re-frame.core :as rf]))

;; functions

(defn fetch-charge-list-by-user [user-id]
  (go
    (let [db-path   [:charge-list]
          user-data (user/data)]
      (rf/dispatch-sync [:set db-path :loading])
      (-> (api-request/call :list-charges {:user-id user-id} user-data)
          <!
          (as-> response
              (let [error (:error response)]
                (if error
                  (println "fetch-charges-by-user error:" error)
                  (rf/dispatch-sync [:set db-path (:charges response)]))))))))

(defn fetch-url-data-to-path [db-path url function]
  (go
    (-> (http/get url)
        <!
        (as-> response
            (let [status (:status response)
                  body   (:body response)]
              (if (= status 200)
                (do
                  (println "retrieved" url)
                  (rf/dispatch [:set db-path (if function
                                               (function body)
                                               body)]))
                (println "error fetching" url)))))))

(defn fetch-charge-and-fill-form [charge-id]
  (go
    (let [form-db-path [:charge-form]
          user-data    (user/data)]
      (rf/dispatch-sync [:set form-db-path :loading])
      (-> (api-request/call :fetch-charge {:id charge-id} user-data)
          <!
          (as-> response
              (if-let [error (:error response)]
                (println ":fetch-charge-by-id error:" error)
                (do
                  (rf/dispatch [:set form-db-path response])
                  (fetch-url-data-to-path (conj form-db-path :data :edn-data)
                                          (:edn-data-url response) reader/read-string)
                  (fetch-url-data-to-path (conj form-db-path :data :svg-data)
                                          (:svg-data-url response) nil))))))))

(defn charge-path [charge-id]
  (str "/charges/#" charge-id))

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


(defn load-svg-file [db-path data]
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
                    width    (-> parsed
                                 (get-in [1 :width])
                                 js/parseFloat)
                    height   (-> parsed
                                 (get-in [1 :height])
                                 js/parseFloat)]
                (rf/dispatch [:set (conj db-path :width) width])
                (rf/dispatch [:set (conj db-path :height) height])
                (rf/dispatch [:set (conj db-path :data) {:edn-data edn-data
                                                         :svg-data data}]))))
      (catch :default e
        (println "error:" e)))))

(defn preview [width height charge-data]
  (let [{:keys [edn-data]} charge-data]
    (when edn-data
      [:svg {:viewBox             (str "0 0 " width " " height)
             :preserveAspectRatio "xMidYMid meet"}
       edn-data])))

(defn upload-file [event db-path]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file db-path raw-data))))
        (.readAsText reader file)))))

(defn save-charge-clicked [db-path]
  (let [payload   @(rf/subscribe [:get db-path])
        user-data (user/data)]
    (go
      (try
        (let [response  (<! (api-request/call :save-charge payload user-data))
              error     (:error response)
              charge-id (-> response :charge-id)]
          (println "save charge response" response)
          (when-not error
            (rf/dispatch [:set (conj db-path :id) charge-id])
            (state/goto (charge-path charge-id))))
        (catch :default e
          (println "save-form error:" e))))))

(defn charge-form []
  (let [db-path                     [:charge-form]
        error-message               @(rf/subscribe [:get-form-error db-path])
        {:keys [width height data]} @(rf/subscribe [:get db-path])
        on-submit                   (fn [event]
                                      (.preventDefault event)
                                      (.stopPropagation event)
                                      (save-charge-clicked db-path))]
    [:div.pure-g
     [:div.pure-u-1-2
      [preview width height data]]
     [:div.pure-u-1-2
      [:form.pure-form.pure-form-aligned {:style        {:display "inline-block"}
                                          :on-key-press (fn [event]
                                                          (when (-> event .-code (= "Enter"))
                                                            (on-submit event)))
                                          :on-submit    on-submit}
       (when error-message
         [:div.error-message error-message])
       [:fieldset
        [form/field (conj db-path :key)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for "key"} "Charge Key"]
            [:input {:id        "key"
                     :value     value
                     :on-change on-change
                     :type      "text"}]])]
        [form/field (conj db-path :name)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for "name"} "Name"]
            [:input {:id        "name"
                     :value     value
                     :on-change on-change
                     :type      "text"}]])]
        [form/field (conj db-path :attitude)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for "attitude"} "Attitude"]
            [:input {:id        "attitude"
                     :value     value
                     :on-change on-change
                     :type      "text"}]])]
        [form/field (conj db-path :data)
         (fn [& _]
           [:div.pure-control-group
            [:label {:for "upload"} "Upload"]
            [:input {:type      "file"
                     :accept    "image/svg+xml"
                     :id        "upload"
                     :on-change #(upload-file % db-path)}]])]]
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [:button.pure-button.pure-button-primary {:type "submit"}
         "Save"]]]]]))

(defn list-charges-for-user []
  (let [user-data   (user/data)
        charge-list @(rf/subscribe [:get [:charge-list]])]
    [:div
     [:h4 "My charges"]
     [:button.pure-button.pure-button-primary {:on-click #(rf/dispatch [:set [:charge-form] {}])}
      "Create"]
     (cond
       (nil? charge-list)       (do
                                  (fetch-charge-list-by-user (:user-id user-data))
                                  [:<>])
       (= charge-list :loading) [:<>]
       :else                    [:ul.charge-list
                                 (doall
                                  (for [charge charge-list]
                                    ^{:key (:id charge)}
                                    [:li.charge
                                     (let [href (str (state/path) "#" (:id charge))]
                                       [:a {:href     href
                                            :on-click #(do
                                                         (.preventDefault %)
                                                         (state/goto href))}
                                        (:name charge) " "
                                        [:i.far.fa-edit]])]))])]))

(defn logged-in []
  (let [charge-form-data @(rf/subscribe [:get [:charge-form]])
        path-extra       (state/path-extra)]
    (cond
      (and path-extra
           (nil? charge-form-data)) (do
                                      (fetch-charge-and-fill-form path-extra)
                                      [:<>])
      (= charge-form-data :loading) [:<>]
      charge-form-data              [charge-form]
      :else                         [list-charges-for-user])))

(defn not-logged-in []
  [:div "You need to be logged in."])

(defn main []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [logged-in]
      [not-logged-in])))
