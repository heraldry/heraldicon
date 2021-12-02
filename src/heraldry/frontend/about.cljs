(ns heraldry.frontend.about
  (:require
   [heraldry.frontend.language :refer [tr]]
   [heraldry.gettext :refer [string]]
   [heraldry.static :as static]
   [re-frame.core :as rf]))

(defn view []
  (rf/dispatch [:set-title (string "About")])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:h2 [tr (string "Welcome to Heraldry Digital")]]
   [:h3 [tr (string "What's this about?")]]
   [:img {:style {:width "30%"
                  :float "right"
                  :margin-left "20px"
                  :margin-bottom "20px"}
          :src (static/static-url "/img/heraldry-digital-logo.png")
          :alt "logo"}]
   [:p
    [:a {:href "https://en.wikipedia.org/wiki/Heraldry"
         :target "_blank"} [tr (string "Heraldry")]]
    " "
    [tr (string "is interesting and fun, but it has a myriad of conventions and rules, as it's been around for almost a millennium.")]]
   [:p
    [tr (string "Creating a coat of arms is a time-consuming process and requires knowledge of various design tools. But even if you put in that time, the result usually is somewhat static, it can't easily be adjusted without more manual work, for instance to change the style, the tinctures, or the shape of the escutcheon.")]]
   [:p
    [tr (string "That is what Heraldry Digital wants to improve. You can describe a coat of arms, and let the rendering figure out the rest. After that it can be themed, the escutcheon can be changed, it can be used for impalement in other coats, charges can be swapped for other variants of a different style, etc..")]]
   [:p
    [tr (string "Obviously it won't replace human skills and artistry, but it might provide a starting point to try out some concepts and generate a base of things that can be automated, e.g. divisions, ordinaries, and line styles.")]]
   [:h3 [tr (string "Goals")]]
   [:ul
    [:li [tr (string "Provide an open charge library, which anyone can add to")]]
    [:li [tr (string "Provide means to create online armories for reference or research")]]
    [:li [tr (string "Support as many heraldic concepts and elements as possible")]]]
   [:h3 [tr (string "Status")]]
   [:p
    [tr (string "This entire website is pretty beta still, so please be patient if you run into bugs or missing features. See my contact details below to report anything weird.")]]
   [:h3 [tr (string "Licensing")]]
   [:p
    [tr (string "Charges and arms made public on this site require a Creative Commons license or be declared in the public domain. Derivative work requires attribution of the source, and the source license must be compatible with the chosen CC license.")]]
   [:p
    [tr (string "You can specify this while editing a charge or arms, and viewing or exporting work will include the license and attribution. It's your responsibility to provide this information, if you make the work public.")]]
   [:p
    [:em
     [tr (string "If you see your work WITHOUT proper attribution or have other concerns or feedback regarding the implementation of these features, then please contact me.")]]]
   [:p
    [tr (string "Note: SVGs get the proper metadata of license/attribution for the coat of arms and all used charges, PNGs are a work in progress.")]]])
