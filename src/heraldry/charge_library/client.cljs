(ns heraldry.charge-library.client
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs.core.async :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.charge-library.api.request :as api-request]
            [hickory.core :as hickory]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

;; subs

(rf/reg-sub
 :get
 (fn [db [_ & args]]
   (get-in db args)))

(rf/reg-sub
 :get-in
 (fn [db [_ path]]
   (get-in db path)))


;; events


(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {} db)))

(rf/reg-event-db
 :set
 (fn [db [_ & args]]
   (assoc-in db (drop-last args) (last args))))

(rf/reg-event-db
 :set-in
 (fn [db [_ path value]]
   (assoc-in db path value)))

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
                (let [svg-data (-> parsed
                                   (assoc 0 :g)
                                   (assoc 1 {}))
                      width (-> parsed
                                (get-in [1 :width]))
                      height (-> parsed
                                 (get-in [1 :height]))]
                  (rf/dispatch [:set-in db-path {:width width
                                                 :height height
                                                 :data svg-data}]))))
      (catch :default e
        (println "error:" e)))))

(defn preview [db-path]
  [:div.preview
   (let [{:keys [width height data]} @(rf/subscribe [:get-in db-path])]
     (when data
       [:svg {:viewBox (str "0 0 " width " " height)
              :preserveAspectRatio "xMidYMid meet"}
        data]))])

(defn upload-file [event db-path]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file db-path raw-data))))
        (.readAsText reader file)))))

(defn form-field [db-path key function]
  (let [field-path (conj db-path key)
        value @(rf/subscribe [:get-in field-path])
        error @(rf/subscribe [:get-in (-> [:form-error-data]
                                          (into db-path)
                                          (conj :errors key))])]
    (-> [:div.field {:class (when error "error")}]
        (cond->
         error (conj [:p.error-message error]))
        (conj (function :value value
                        :on-change #(let [new-value (-> % .-target .-value)]
                                      (rf/dispatch [:set-in field-path new-value])))))))

(defn save-charge-form [db-path]
  (let [payload @(rf/subscribe [:get-in db-path])]
    (go
      (try
        (let [response (<! (api-request/call :save-charge payload))])
        (catch :default e
          (println "save-form error:" e))))))

(defn charge-form [db-path]
  (let [error-data @(rf/subscribe [:get-in (into [:form-error-data] db-path)])
        error-message (:message error-data)]
    [:div
     [preview (conj db-path :charge/data)]
     [:div.form
      (when error-message
        [:div.error-top
         [:p.error-message error-message]])
      [form-field db-path :charge/key
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "key"} "Charge Key"]
          [:input {:id "key"
                   :value value
                   :on-change on-change
                   :type "text"}]])]
      [form-field db-path :charge/name
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "name"} "Name"]
          [:input {:id "name"
                   :value value
                   :on-change on-change
                   :type "text"}]])]
      [form-field db-path :charge/attitude
       (fn [& {:keys [value on-change]}]
         [:<>
          [:label {:for "attitude"} "Attitude"]
          [:input {:id "attitude"
                   :value value
                   :on-change on-change
                   :type "text"}]])]
      [form-field db-path :charge/data
       (fn [& _]
         [:<>
          [:label {:for "upload"
                   :style {:cursor "pointer"}} "Upload"]
          [:input {:type "file"
                   :accept "image/svg+xml"
                   :id "upload"
                   :on-change #(upload-file % (conj db-path :charge/data))}]])]
      [:div.buttons
       [:button.save {:on-click #(save-charge-form db-path)} "Save"]]]]))

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (r/render [charge-form [:charge-form]]
            (.getElementById js/document "app")))

(defn ^:export init []
  (start))
