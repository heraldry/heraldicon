(ns heraldry.charge-library.client
  (:require [re-frame.core :as rf]
            [reagent.dom :as r]))

;; events

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {} db)))

;; views

(defn charge-form [])

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (r/render [charge-form]
            (.getElementById js/document "app")))

(defn ^:export init []
  (start))
