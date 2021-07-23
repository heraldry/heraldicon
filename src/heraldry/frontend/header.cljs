(ns heraldry.frontend.header
  (:require [heraldry.frontend.route :as route]
            [heraldry.frontend.user :as user]))

(defn view []
  (let [user-data (user/data)]
    [:div.header
     [:div {:style {:flex 1.5
                    :line-height "2em"
                    :padding-left "1em"}}
      [:span {:style {:text-transform "uppercase"}}
       [route/link {:to :home
                    :style {:padding-right "5px"}} "Heraldry"]]
      [:sup {:style {:color "#d82"}} "beta"]]
     [:ul.nav-menu {:style {:flex 1}}
      [route/nav-link {:to :home} "Home"]
      [route/nav-link {:to :collections} "Collections"]
      [route/nav-link {:to :arms} "Arms"]
      [route/nav-link {:to :charges} "Charges"]
      [route/nav-link {:to :users} "Users"]
      [route/nav-link {:to :about} "About"]
      [:span {:style {:width "5em"}}]
      (if (:logged-in? user-data)
        [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
         {:style {:min-width "6em"}}
         [:<>
          [:a.nav-menu-link {:href "#"} (str "@" (:username user-data))]
          [:ul.nav-menu.nav-menu-children
           [route/nav-link {:to :account} "Account"]
           [:li.nav-menu-item
            [:a.nav-menu-link {:href "#"
                               :on-click #(user/logout)} "Logout"]]]]]
        [:<>
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(user/login-modal)} "Login"]]
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(user/sign-up-modal)} "Register"]]])]]))
