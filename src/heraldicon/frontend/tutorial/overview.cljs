(ns heraldicon.frontend.tutorial.overview
  (:require
   [heraldicon.frontend.tutorial :as tutorial]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(def goals
  [{:title :string.tutorial.overview/welcome-title
    :description :string.tutorial.overview/welcome-description}

   {:title :string.tutorial.overview/navigation-title
    :description :string.tutorial.overview/navigation-description
    :hints [{:element "[data-tour='nav-menu']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/arms-library-title
    :description :string.tutorial.overview/arms-library-description
    :hints [{:element "[data-tour='arms-library']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/charge-library-title
    :description :string.tutorial.overview/charge-library-description
    :hints [{:element "[data-tour='charge-library']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/collection-library-title
    :description :string.tutorial.overview/collection-library-description
    :hints [{:element "[data-tour='collection-library']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/ribbon-library-title
    :description :string.tutorial.overview/ribbon-library-description
    :hints [{:element "[data-tour='ribbon-library']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/login-title
    :description :string.tutorial.overview/login-description
    :hints [{:element "[data-tour='login-register']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/news-title
    :description :string.tutorial.overview/news-description
    :hints [{:element "[data-tour='news']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/atom-feed-title
    :description :string.tutorial.overview/atom-feed-description
    :hints [{:element "[data-tour='atom-feed']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/support-title
    :description :string.tutorial.overview/support-description
    :hints [{:element "[data-tour='ko-fi']"
             :side "bottom"}]}

   {:title :string.tutorial.overview/finish-title
    :description :string.tutorial.overview/finish-description}])

(tutorial/register-tour! :overview {:goals goals})

(rf/reg-event-fx ::start
  (fn [_ _]
    (reife/push-state :route.home/main)
    {:dispatch [::tutorial/start :overview]}))
