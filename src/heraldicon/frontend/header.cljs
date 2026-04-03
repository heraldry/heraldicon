(ns heraldicon.frontend.header
  (:require
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.dark-mode :as dark-mode]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.ko-fi :as ko-fi]
   [heraldicon.frontend.language :as language :refer [tr]]
   [heraldicon.frontend.router :as router]
   [heraldicon.frontend.tutorial.arms :as tutorial.arms]
   [heraldicon.frontend.tutorial.overview :as tutorial.overview]
   [heraldicon.frontend.user.form.login :as form.login]
   [heraldicon.frontend.user.form.register :as form.register]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.static :as static]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(def ^:private user-menu-open?-path
  [:ui :menu :user-menu :open?])

(def ^:private tutorial-menu-open?-path
  [:ui :menu :tutorial-menu :open?])

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

(defn- menu-item [route name & {:keys [on-click
                                       highlight-active?]
                                :or {highlight-active? true}}]
  [:li.nav-menu-item {:class (when (and highlight-active?
                                        (router/active-section? route))
                               "selected")}
   [:a {:href (reife/href route nil nil)
        :on-click on-click} [tr name]]])

(defn view []
  (let [session @(rf/subscribe [::session/data])
        logged-in? @(rf/subscribe [::session/logged-in?])]
    [:div.header {:class (dark-mode/class)}
     [:div {:style {:flex 1.5
                    :line-height "2em"
                    :padding-left "0.5em"
                    :white-space "nowrap"}}
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
     [:ul.nav-menu {:data-tour "nav-menu"
                    :style {:flex 1}}
      [:li {:data-tour "ko-fi"
            :style {:margin "auto"}}
       [ko-fi/small-button]]
      [:li {:data-tour "atom-feed"
            :style {:margin "auto"}}
       [:a {:href "/atom.xml"
            :target "_blank"
            :title "Atom Feed"
            :style {:padding-right "0.5em"}}
        [:i.fas.fa-rss-square]]]
      [menu-item :route.home/main :string.menu/about]
      [:li.nav-menu-item {:data-tour "news"
                          :class (when (router/active-section? :route.news/main)
                                   "selected")}
       [:a {:href (reife/href :route.news/main nil nil)}
        [tr :string.menu/news]]]
      [:li.nav-menu-item {:data-tour "collection-library"
                          :class (when (router/active-section? :route.collection/list)
                                   "selected")}
       [:a {:href (reife/href :route.collection/list nil nil)}
        [tr :string.menu/collection-library]]]
      [:li.nav-menu-item {:data-tour "arms-library"
                          :class (when (router/active-section? :route.arms/list)
                                   "selected")}
       [:a {:href (reife/href :route.arms/list nil nil)}
        [tr :string.menu/arms-library]]]
      [:li.nav-menu-item {:data-tour "charge-library"
                          :class (when (router/active-section? :route.charge/list)
                                   "selected")}
       [:a {:href (reife/href :route.charge/list nil nil)}
        [tr :string.menu/charge-library]]]
      [:li.nav-menu-item {:data-tour "ribbon-library"
                          :class (when (router/active-section? :route.ribbon/list)
                                   "selected")}
       [:a {:href (reife/href :route.ribbon/list nil nil)}
        [tr :string.menu/ribbon-library]]]
      (when (entity.user/admin? session)
        [:<>
         [menu-item :route.user/list :string.menu/users]
         [menu-item :route.charge-types/main :string.menu/charge-types]])
      [menu-item :route.contact/main :string.menu/contact]
      [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
       {:on-mouse-leave #(rf/dispatch [::clear-menu-open? tutorial-menu-open?-path])}
       [:<>
        [:a.nav-menu-link {:href "#"
                           :on-click (js-event/handled #(rf/dispatch [::toggle-menu-open? tutorial-menu-open?-path]))}
         [:i.fas.fa-question-circle] " Tutorial"]
        [:ul.nav-menu.nav-menu-children
         {:style {:display (if @(rf/subscribe [::menu-open?
                                               tutorial-menu-open?-path])
                             "block"
                             "none")}}
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click (js-event/handled
                                        #(do (rf/dispatch [::clear-menu-open? tutorial-menu-open?-path])
                                             (rf/dispatch [::tutorial.overview/start])))}
           "Overview"]]
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click (js-event/handled
                                        #(do (rf/dispatch [::clear-menu-open? tutorial-menu-open?-path])
                                             (rf/dispatch [::tutorial.arms/start])))}
           "Coat of Arms Editor"]]]]]
      [:span {:style {:width "5em"}}]
      [language/selector]
      [:span {:style {:width "1em"}}]
      [dark-mode/selector]
      (if logged-in?
        [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
         {:data-tour "user-menu"
          :on-mouse-leave #(rf/dispatch [::clear-menu-open? user-menu-open?-path])}
         [:<>
          [:a.nav-menu-link {:style {:min-width "6em"}
                             :href "#"
                             :on-click (js-event/handled #(rf/dispatch [::toggle-menu-open? user-menu-open?-path]))}
           (str "@" (:username session) " ")]
          [:ul.nav-menu.nav-menu-children
           {:style {:display (if @(rf/subscribe [::menu-open?
                                                 user-menu-open?-path])
                               "block"
                               "none")}}
           [menu-item :route.account/main :string.menu/account
            :on-click #(rf/dispatch [::clear-menu-open? user-menu-open?-path])
            :highlight-active? false]
           [:li.nav-menu-item
            [:a.nav-menu-link {:href "#"
                               :on-click (js-event/handled
                                          #(do (rf/dispatch [::clear-menu-open? user-menu-open?-path])
                                               (rf/dispatch [::session/logout])))}
             [tr :string.menu/logout]]]]]]
        [:<>
         [:li.nav-menu-item {:data-tour "login-register"}
          [:a.nav-menu-link {:href "#"
                             :on-click (js-event/handled #(rf/dispatch [::form.login/show]))}
           [tr :string.menu/login]]]
         [:li.nav-menu-item
          [:a.nav-menu-link {:href "#"
                             :on-click (js-event/handled #(rf/dispatch [::form.register/show]))}
           [tr :string.menu/register]]]])]]))
