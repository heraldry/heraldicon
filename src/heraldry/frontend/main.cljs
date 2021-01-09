(ns heraldry.frontend.main
  (:require [re-frame.core :as rf]
            [reagent.dom :as r]))

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {} db)))

(defn app []
  [:div])

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  #_(rf/dispatch-sync [:set [:user-data] (user/load-session-user-data)])
  (r/render
   [app]
   (.getElementById js/document "app")))

(defn ^:export init []
  (start))
