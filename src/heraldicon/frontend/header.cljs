(ns heraldicon.frontend.header
  (:require
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.language :as language :refer [tr]]
   [heraldicon.frontend.router :as router]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user.form.login :as form.login]
   [heraldicon.frontend.user.form.register :as form.register]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.static :as static]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

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

(defn- menu-item [route name]
  [:li.nav-menu-item {:class (when (router/active-section? route)
                               "selected")}
   [:a {:href (reife/href route nil nil)} [tr name]]])

(defn view []
  (let [user-data @(rf/subscribe [::session/data])]
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
       [:a {:href (reife/href :route.home/main nil nil)
            :style {:padding-right "5px"}} "Heraldicon"]]
      [:sup {:style {:color "#d82"}} "beta"]]
     [:ul.nav-menu {:style {:flex 1}}
      [menu-item :route.home/main :string.menu/about]
      [menu-item :route.news/main :string.menu/news]
      [menu-item :route.collection/list :string.menu/collection-library]
      [menu-item :route.arms/list :string.menu/arms-library]
      [menu-item :route.charge/list :string.menu/charge-library]
      [menu-item :route.ribbon/list :string.menu/ribbon-library]
      (when (entity.user/admin? user-data)
        [menu-item :route.user/list :string.menu/users])
      [menu-item :route.contact/main :string.menu/contact]
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
           [menu-item :route.account/main :string.menu/account]
           [:li.nav-menu-item
            [:a.nav-menu-link {:href "#"
                               :on-click #(rf/dispatch [::session/logout])} [tr :string.menu/logout]]]]]]
        [:<>
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(rf/dispatch [::form.login/show])} [tr :string.menu/login]]]
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click #(rf/dispatch [::form.register/show])} [tr :string.menu/register]]]])]]))
