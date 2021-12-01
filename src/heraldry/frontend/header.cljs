(ns heraldry.frontend.header
  (:require
   [heraldry.config :as config]
   [heraldry.frontend.language :as language :refer [tr]]
   [heraldry.frontend.route :as route]
   [heraldry.frontend.user :as user]
   [heraldry.gettext :refer [string]]
   [heraldry.static :as static]
   [heraldry.strings :as strings]))

(defn view []
  (let [user-data (user/data)]
    [:div.header
     [:div {:style {:flex 1.5
                    :line-height "2em"
                    :padding-left "0.5em"}}
      [:img {:src (static/static-url "/img/heraldry-digital-logo.png")
             :style {:height "1.8em"
                     :margin-right "0.25em"
                     :position "relative"
                     :transform "translate(0,0.4em)"}}]
      [:span {:style {:text-transform "uppercase"}}
       [route/link {:to :home
                    :style {:padding-right "5px"}} "Heraldry"]]
      [:sup {:style {:color "#d82"}} "beta"]]
     [:ul.nav-menu {:style {:flex 1}}
      [route/nav-link {:to :home} [tr (string "News")]]
      [route/nav-link {:to :about} [tr (string "About")]]
      [route/nav-link {:to :collections} [tr strings/collections]]
      [route/nav-link {:to :arms} [tr strings/arms]]
      [route/nav-link {:to :charges} [tr strings/charges]]
      [route/nav-link {:to :ribbons} [tr strings/ribbons]]
      (when (-> user-data :username ((config/get :admins)))
        [route/nav-link {:to :users} [tr (string "Users")]])
      [route/nav-link {:to :contact} [tr (string "Contact")]]
      [:span {:style {:width "5em"}}]
      [:li [language/selector]]
      (if (:logged-in? user-data)
        [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
         {:style {:min-width "6em"}}
         [:<>
          [:a.nav-menu-link {:href "#"} (str "@" (:username user-data))]
          [:ul.nav-menu.nav-menu-children
           [route/nav-link {:to :account} [tr (string "Account")]]
           [:li.nav-menu-item
            [:a.nav-menu-link {:href "#"
                               :on-click #(user/logout)} [tr (string "Logout")]]]]]]
        [:<>
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(user/login-modal)} [tr (string "Login")]]]
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(user/sign-up-modal)} [tr (string "Register")]]]])]]))
