(ns heraldicon.frontend.home
  (:require
   [heraldicon.frontend.title :as title]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(def ^:private heraldicon
  [:span {:style {:font-family "\"Trajan Pro\", sans-serif"
                  :font-size "1em"}}
   "Heraldicon"])

(defn view []
  (rf/dispatch [::title/set :string.menu/about])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:h2 "Welcome to " heraldicon]
   [:h3 "Introduction"]
   [:img {:style {:width "30%"
                  :float "right"
                  :margin-left "20px"
                  :margin-bottom "20px"}
          :src (static/static-url "/img/heraldicon-logo.png")
          :alt "logo"}]
   [:p
    [:a {:href "https://en.wikipedia.org/wiki/Heraldry"
         :target "_blank"} "Heraldry"]
    " "
    "is interesting and fun, but it has a myriad of conventions and rules, as it's been around for almost a millennium."]
   [:p
    "Creating a coat of arms is a time-consuming process and requires knowledge of various design tools. But even if you put in that time, the result usually is somewhat static, it can't easily be adjusted without more manual work, for instance to change the style, the tinctures, or the shape of the shield."]
   [:p
    "With " heraldicon " you can describe a coat of arms, and let the rendering figure out the rest. After that it can be themed, the shield can be changed, it can be used for impalement in other coats of arms, charges can be swapped for other variants of a different style, and so on."]
   [:p
    "It won't replace human skills and artistry, but it might provide a starting point to try out some concepts and generate a foundation of things that can be automated, e.g. divisions, ordinaries, and line styles."]
   [:h3 "Goals"]
   [:ul
    [:li "Support as many heraldic concepts and elements as possible"]
    [:li "Provide an open charge library, which anyone can add to and use"]
    [:li "Provide means to create online armories for reference or research"]]
   [:h3 "Status"]
   [:p
    "This entire website is a work in progress, so please be patient if you run into bugs or missing features. See the contact page to report anything weird."]
   [:h3 "Licensing"]
   [:p
    "Entities made public on this site require a Creative Commons license or be declared in the public domain. Derivative work requires attribution of the source, and the source license must be compatible with the chosen CC license."]
   [:p
    "It's your responsibility to provide attribution and a license, if you make the work public."]
   [:p
    [:em
     "If you see your work WITHOUT proper attribution or have other concerns or feedback regarding the implementation of these features, then please contact me."]]])
