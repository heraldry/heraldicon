(ns heraldry.frontend.main
  (:require [heraldry.frontend.modal :as modal]
            [heraldry.frontend.route :as route]
            [heraldry.frontend.user :as user]
            [re-frame.core :as rf]
            [reagent.dom :as r]))

(defn header []
  (let [user-data (user/data)]
    [:div.header
     [:div.home-menu.pure-menu.pure-menu-horizontal.pure-menu-fixed
      [:span [route/link {:to    :home
                          :class "pure-menu-heading pure-float-right"
                          :style {:padding-right "5px"}} "Heraldry"]
       [:sup {:style {:color "#d82"}} "beta"]]
      [:ul.pure-menu-list
       [route/nav-link {:to :home} "Home"]
       #_[route/nav-link {:to :collections} "Collections"]
       [route/nav-link {:to :arms} "Arms"]
       [route/nav-link {:to :charges} "Charges"]
       [route/nav-link {:to :about} "About"]
       [:span.horizontal-spacer {:style {:width "5em"}}]
       (if (:logged-in? user-data)
         [:li.pure-menu-item.pure-menu-has-children.pure-menu-allow-hover
          {:style {:min-width "6em"}}
          [:<>
           [:a.pure-menu-link {:href "#"} (str "@" (:username user-data))]
           [:ul.pure-menu-children
            [route/nav-link {:to :account} "Account"]
            [:li.pure-menu-item
             [:a.pure-menu-link {:href     "#"
                                 :on-click #(user/logout)} "Logout"]]]]]
         [:<>
          [:li.pure-menu-item
           [:a.pure-menu-link {:href     "#"
                               :on-click #(user/login-modal)} "Login"]]
          [:li.pure-menu-item
           [:a.pure-menu-link {:href     "#"
                               :on-click #(user/sign-up-modal)} "Register"]]])]]]))

(defn app []
  [:<>
   [header]
   [:div.main-content
    (if-let [view (route/view)]
      view
      [:div "Not found"])
    [modal/render]]])

(defn stop [])

(defn start []
  (rf/dispatch-sync [:initialize-db])
  (route/start-router)
  (user/load-session-user-data)
  (r/render
   [app]
   (.getElementById js/document "app")))

(defn ^:export init []
  (start))

