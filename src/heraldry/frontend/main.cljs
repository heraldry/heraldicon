(ns heraldry.frontend.main
  (:require [heraldry.frontend.subs]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:site {:menu {:items [["Home" "/"]
                                 ["Armory" "/armory/"]
                                 ["Charge Library" "/charges/"]]}}} db)))

(defn header [title]
  (let [menu @(rf/subscribe [:get [:site :menu]])
        items (:items menu)
        known-paths (set (map second items))
        path js/location.pathname
        selected (if (get known-paths path)
                   path
                   "/")]
    [:div.header
     [:div.home-menu.pure-menu.pure-menu-horizontal.pure-menu-fixed
      [:a.pure-menu-heading.pure-float-right {} "Heraldry"]
      [:span title]
      [:ul.pure-menu-list
       (for [[name path] items]
         ^{:key path}
         [:li.pure-menu-item {:class (when (= path selected)
                                       "pure-menu-selected")}
          [:a.pure-menu-link {:href path} name]])]]]))

(defn app []
  [:div [header ""]])

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
