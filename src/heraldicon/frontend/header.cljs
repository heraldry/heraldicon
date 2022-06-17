(ns heraldicon.frontend.header
  (:require
   [heraldicon.config :as config]
   [heraldicon.frontend.language :as language :refer [tr]]
   [heraldicon.frontend.route :as route]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(def ^:private user-menu-open?-path
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
                     :margin-right "0.6em"
                     :position "relative"
                     :transform "translate(0,0.4em)"}}]
      [:span {:style {:font-family "\"Trajan Pro\", sans-serif"
                      :font-size "1.2em"}}
       [route/link {:to :route.home/main
                    :style {:padding-right "5px"}} "Heraldicon"]]
      [:sup {:style {:color "#d82"}} "beta"]]
     [:ul.nav-menu {:style {:flex 1}}
      [route/nav-link {:to :route.home/main} [tr :string.menu/about]]
      [route/nav-link {:to :route.news/main} [tr :string.menu/news]]
      [route/nav-link {:to :route.collection/list} [tr :string.menu/collection-library]]
      [route/nav-link {:to :route.arms/list} [tr :string.menu/arms-library]]
      [route/nav-link {:to :route.charge/list} [tr :string.menu/charge-library]]
      [route/nav-link {:to :route.ribbon/list} [tr :string.menu/ribbon-library]]
      (when (-> user-data :username ((config/get :admins)))
        [route/nav-link {:to :route.user/list} [tr :string.menu/users]])
      [route/nav-link {:to :route.contact/main} [tr :string.menu/contact]]
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
           [route/nav-link {:to :route.account/main} [tr :string.menu/account]]
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
