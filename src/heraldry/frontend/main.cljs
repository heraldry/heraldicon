(ns heraldry.frontend.main
  (:require [clojure.string :as s]
            [heraldry.frontend.charge-library :as charge-library]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

(rf/reg-event-db
 :initialize-db
 (fn [db [_]]
   (merge {:render-options {:component :render-options
                            :mode :colours
                            :outline? false
                            :squiggly? false
                            :ui {:selectable-fields? true}}
           :ui {:component-open? {[:render-options] true}}
           :site {:menu {:items [["Home" "/"]
                                 ["Armory" "/armory/"]
                                 ["Charge Library" "/charges/"]]}}} db)))

(defn header []
  (let [user-data (user/data)
        menu @(rf/subscribe [:get [:site :menu]])
        items (:items menu)
        known-paths (set (map second items))
        path (state/path)
        selected (if (get known-paths path)
                   path
                   "/")]
    [:div.header
     [:div.home-menu.pure-menu.pure-menu-horizontal.pure-menu-fixed
      [:a.pure-menu-heading.pure-float-right {} "Heraldry"]
      [:ul.pure-menu-list
       (for [[name path] items]
         ^{:key path}
         [:li.pure-menu-item {:class (when (= path selected)
                                       "pure-menu-selected")}
          [:a.pure-menu-link {:on-click #(state/goto path)} name]])
       [:span.spacer {:style {:width "5em"}}]
       (if (:logged-in? user-data)
         [:li.pure-menu-item.pure-menu-has-children.pure-menu-allow-hover
          {:style {:min-width "6em"}}
          [:<>
           [:a.pure-menu-link {} (str "@" (:username user-data))]
           [:ul.pure-menu-children
            [:li.pure-menu-item
             [:a.pure-menu-link {} "Settings"]]
            [:li.pure-menu-item
             [:a.pure-menu-link {:on-click #(user/logout)} "Logout"]]]]]
         [:<>
          [:li.pure-menu-item
           [:a.pure-menu-link {:on-click #(user/login-modal)} "Login"]]
          [:li.pure-menu-item
           [:a.pure-menu-link {:on-click #(user/sign-up-modal)} "Register"]]])]]]))

(defn app []
  (let [path (state/path)]
    [:<>
     [header]
     [:div.main-content
      (cond
        (s/starts-with? path "/charges/") (charge-library/main))]
     [modal/render]]))

(defn stop []
  (println "Stopping..."))

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (user/load-session-user-data)
  (state/set-path js/location.pathname js/location.hash)
  (r/render
   [app]
   (.getElementById js/document "app")))

(defn ^:export init []
  (start))
