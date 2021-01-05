(ns heraldry.charge-library.client
  (:require ["svgo-browser/lib/get-svgo-instance" :as getSvgoInstance]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [clojure.string :as s]
            [clojure.walk :as walk]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
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


(defn load-svg-file [data]
  (go
    (try
      (-> data
          optimize-svg
          <?
          hickory/parse-fragment
          first
          hickory/as-hiccup
          (as-> parsed
              (rf/dispatch [:set-in [:charge :data] parsed])))
      (catch :default e
        (println "error:" e)))))

(defn preview []
  [:div.preview
   (when-let [data @(rf/subscribe [:get-in [:charge :data]])]
     (let [width  (-> data second :width)
           height (-> data second :height)
           data   (-> data
                      (update-in [1] dissoc :width :height)
                      (assoc-in [1 :viewBox] (str "0 0 " width " " height))
                      (assoc-in [1 :preserveAspectRatio] "xMidYMid meet"))]
       data))])

(defn upload-file [event]
  (let [file (-> event .-target .-files (.item 0))]
    (when file
      (let [reader (js/FileReader.)]
        (set! (.-onloadend reader) (fn []
                                     (let [raw-data (.-result reader)]
                                       (load-svg-file raw-data))))
        (.readAsText reader file)))))

(defn charge-form []
  [:div
   [preview]
   [:div.form
    [:div.setting
     [:label {:for "charge"} "Charge"]
     [:input {:id   "charge"
              :type "text"}]]
    [:div.setting
     [:label {:for "name"} "Name"]
     [:input {:id   "name"
              :type "text"}]]
    [:div.setting
     [:label {:for "attitude"} "Attitude"]
     [:input {:id   "attitude"
              :type "text"}]]
    [:div.setting
     [:label {:for   "upload"
              :style {:cursor "pointer"}} "Upload"]
     [:input {:type      "file"
              :accept    "image/svg+xml"
              :id        "upload"
              :on-change upload-file}]]
    [:div.buttons
     [:button.save "Save"]
     [:button.save "Upload"]]]])

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (r/render [charge-form]
            (.getElementById js/document "app")))

(defn ^:export init []
  (start))
