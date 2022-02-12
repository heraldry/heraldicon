(ns heraldry.frontend.header
  (:require
   [heraldry.config :as config]
   [heraldry.frontend.language :as language :refer [tr]]
   [heraldry.frontend.route :as route]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.user :as user]
   [heraldry.static :as static]
   [re-frame.core :as rf]))

(def user-menu-open?-path
  [:ui :menu :user-menu :open?])

(rf/reg-sub ::menu-open?
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [open? _]
    open?))

(rf/reg-event-db ::toggle-menu-open?
  (fn [db [_ path]]
    (update-in db path not)))

(rf/reg-event-db ::clear-menu-open?
  (fn [db [_ path]]
    (assoc-in db path nil)))

(defn view []
  (let [user-data (user/data)]
    [:div.header
     [:div {:style {:flex 1.5
                    :line-height "2em"
                    :padding-left "0.5em"}}
      [:img {:src (static/static-url "/img/heraldicon-logo.png")
             :style {:height "1.8em"
                     :margin-right "0.25em"
                     :position "relative"
                     :transform "translate(0,0.4em)"}}]
      [:span {:style {:font-family "\"Trajan Pro\", sans-serif"
                      :font-size "1.2em"}}
       [route/link {:to :home
                    :style {:padding-right "5px"}} "Heraldicon"]]
      [:sup {:style {:color "#d82"}} "beta"]]
     [:ul.nav-menu {:style {:flex 1}}
      [route/nav-link {:to :home} [tr :string.menu/about]]
      [route/nav-link {:to :news} [tr :string.menu/news]]
      [route/nav-link {:to :collections} [tr :string.menu/collection-library]]
      [route/nav-link {:to :arms} [tr :string.menu/arms-library]]
      [route/nav-link {:to :charges} [tr :string.menu/charge-library]]
      [route/nav-link {:to :ribbons} [tr :string.menu/ribbon-library]]
      (when (-> user-data :username ((config/get :admins)))
        [route/nav-link {:to :users} [tr :string.menu/users]])
      [route/nav-link {:to :contact} [tr :string.menu/contact]]
      [:span {:style {:width "5em"}}]
      [language/selector]
      [:span {:style {:width "1em"}}]
      (if (:logged-in? user-data)
        [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
         {:on-mouse-leave #(rf/dispatch [::clear-menu-open?
                                         user-menu-open?-path])}
         [:<>
          [:a.nav-menu-link {:style {:min-width "6em"}
                             :href "#"
                             :on-click #(state/dispatch-on-event-and-prevent-default
                                         % [::toggle-menu-open?
                                            user-menu-open?-path])}
           (str "@" (:username user-data) " ")]
          [:ul.nav-menu.nav-menu-children
           {:style {:display (if @(rf/subscribe [::menu-open?
                                                 user-menu-open?-path])
                               "block"
                               "none")}}
           [route/nav-link {:to :account} [tr :string.menu/account]]
           [:li.nav-menu-item
            [:a.nav-menu-link {:href "#"
                               :on-click #(user/logout)} [tr :string.menu/logout]]]]]]
        [:<>
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(user/login-modal)} [tr :string.menu/login]]]
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(user/sign-up-modal)} [tr :string.menu/register]]]])]]))
