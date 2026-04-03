(ns heraldicon.frontend.tutorial.overview
  (:require
   [heraldicon.frontend.tutorial :as tutorial]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]))

(def goals
  [{:title "Welcome to Heraldicon"
    :description "Heraldicon is a platform for heraldry and vexillology. This quick tour will show you around the main features."}

   {:title "Navigation"
    :description "These are the main sections of Heraldicon. You can browse existing coats of arms, charges, collections, and ribbons."
    :hints [{:element "[data-tour='nav-menu']"
             :side "bottom"}]}

   {:title "Arms Library"
    :description "The Arms Library is where you can browse coats of arms that others have created, and create your own."
    :hints [{:element "[data-tour='arms-library']"
             :side "bottom"}]}

   {:title "Charge Library"
    :description "Charges are the figures and symbols used in heraldry — lions, eagles, crosses, and many more. Browse and upload them here."
    :hints [{:element "[data-tour='charge-library']"
             :side "bottom"}]}

   {:title "Collection Library"
    :description "Collections let you group coats of arms together, for example to create an armorial or a roll of arms."
    :hints [{:element "[data-tour='collection-library']"
             :side "bottom"}]}

   {:title "Ribbon Library"
    :description "Ribbons are scroll-like elements used for mottos and slogans. You can design your own ribbon shapes and add text that flows along them."
    :hints [{:element "[data-tour='ribbon-library']"
             :side "bottom"}]}

   {:title "Login / Register"
    :description "You can explore and use everything without an account. But to save your work, you'll need to register and log in. You can find login and registration in the top right."
    :hints [{:element "[data-tour='login-register']"
             :side "bottom"}]}

   {:title "News"
    :description "Check the news section for announcements about new features and other updates."
    :hints [{:element "[data-tour='news']"
             :side "bottom"}]}

   {:title "Atom Feed"
    :description "Subscribe to the Atom feed to get notified about news in your feed reader, so you don't miss anything."
    :hints [{:element "[data-tour='atom-feed']"
             :side "bottom"}]}

   {:title "Support Heraldicon"
    :description "If you enjoy the site and can easily afford it, please consider a donation on Ko-fi — ideally a recurring one. It helps cover hosting and development costs and keeps Heraldicon running."
    :hints [{:element "[data-tour='ko-fi']"
             :side "bottom"}]}

   {:title "That's the overview!"
    :description "To learn more, open the Tutorial menu again and choose one of the other tutorials."}])

(tutorial/register-tour! :overview {:goals goals})

(rf/reg-event-fx ::start
  (fn [_ _]
    (reife/push-state :route.home/main)
    {:dispatch [::tutorial/start :overview]}))
