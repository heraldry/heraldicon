(ns heraldry.frontend.header
  (:require [heraldry.frontend.route :as route]
            [heraldry.frontend.user :as user]))

(defn view []
  (let [user-data (user/data)]
    [:div.header
     [:div.home-menu.pure-menu.pure-menu-horizontal.pure-menu-fixed
      [:span [route/link {:to :home
                          :class "pure-menu-heading pure-float-right"
                          :style {:padding-right "5px"}} "Heraldry"]
       [:sup {:style {:color "#d82"}} "beta"]]
      [:ul.pure-menu-list
       [route/nav-link {:to :home} "Home"]
       [route/nav-link {:to :collections} "Collections"]
       [route/nav-link {:to :arms} "Arms"]
       [route/nav-link {:to :charges} "Charges"]
       [route/nav-link {:to :users} "Users"]
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
             [:a.pure-menu-link {:href "#"
                                 :on-click #(user/logout)} "Logout"]]]]]
         [:<>
          [:li.pure-menu-item
           [:a.pure-menu-link {:href "#"
                               :on-click #(user/login-modal)} "Login"]]
          [:li.pure-menu-item
           [:a.pure-menu-link {:href "#"
                               :on-click #(user/sign-up-modal)} "Register"]]])]]]))
