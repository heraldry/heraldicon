(ns heraldry.frontend.about
  (:require [heraldry.static :as static]
            [re-frame.core :as rf]))

(defn view []
  (rf/dispatch [:set-title "About"])
  [:div {:style {:padding "10px"
                 :text-align "justify"
                 :min-width "30em"
                 :max-width "60em"
                 :margin "auto"}}
   [:h2 "Welcome to Heraldry Digital"]
   [:h3 "What's this about?"]
   [:img {:style {:width "30%"
                  :float "right"
                  :margin-left "20px"
                  :margin-bottom "20px"}
          :src (static/static-url "/img/heraldry-digital-logo.png")
          :alt "release update overview"}]
   [:p
    [:a {:href "https://en.wikipedia.org/wiki/Heraldry"
         :target "_blank"} "Heraldry"] " is interesting and fun, but it has a myriad of conventions and "
    "rules, as it's been around for almost a millennium."]
   [:p
    "Creating a coat of arms is a time-consuming process and requires knowledge of various design tools. "
    "But even if you put in that time, the result usually is somewhat static, it can't easily be adjusted without "
    "more manual work, for instance to change the style, the tinctures, or the shape of the escutcheon."]
   [:p
    "That is what heraldry.digital wants to improve. You can " [:i "describe"] " a coat of arms, and let the rendering "
    "figure out the rest. After that it can be themed, the escutcheon can be changed, it can be used for impalement in "
    "other coats, charges can be swapped for other variants of a different style, etc.."]
   [:p
    "Obviously it won't replace human skills and artistry, but it might provide a starting point "
    "to try out some concepts and generate a base of things that can be automated, "
    "e.g. divisions, ordinaries, and line styles."]
   [:h3 "Goals"]
   [:ul
    [:li "Provide an open charge library, which anyone can add to"]
    [:li "Provide means to create online armories for reference or research"]
    [:li "Support as many heraldic concepts and elements as possible"]]
   [:h3 "Status"]
   [:p
    "This entire website is pretty beta still, so please be patient "
    "if you run into bugs or missing features. See my contact details below to report anything weird."]
   [:h3 "Licensing"]
   [:p
    "Charges and arms made public on this site " [:em "require a Creative Commons license"]
    " or be declared in the public domain. Derivative work "
    [:em "requires attribution of the source"] ", and the source license must be compatible with the chosen "
    "CC license."]
   [:p
    "You can specify this while editing a charge or arms, and viewing or exporting work will include "
    "the license and attribution. It's your responsibility to provide this information, if you make the work public."]
   [:p
    [:em
     "If you see your work without proper attribution or have other concerns or feedback regarding the "
     "implementation of these features, then please contact me."]]
   [:p
    "Note: SVGs get the proper metadata of license/attribution for the coat of arms and all used "
    "charges, PNGs are a work in progress."]
   [:h3 "Contact"]
   [:p "The project is on " [:a {:href "https://github.com/heraldry" :target "_blank"} "github:heraldry"]
    " if you want to get involved in some way."]
   [:p
    "You can email me at oliver [at] heraldry.digital for feedback, questions, complaints, feature "
    "requests, or deep and meaningful friendship."]
   [:p "I can also be found on:"]
   [:ul
    [:li [:a {:href "https://github.com/or" :target "_blank"} "github:or"]]
    [:li [:a {:href "https://www.reddit.com/r/heraldry/" :target "_blank"} "reddit/heraldry"] ", where my username is " [:em "tierced"]]
    [:li [:a {:href "https://discord.gg/PfGrYSz8" :target "_blank"} "discord:heraldry.digital"] ", where my username is " [:em "or#5915"]]
    [:li [:a {:href "https://discord.gg/QFrsdMkbbj" :target "_blank"} "discord:heraldry"] ", which is operated by the heraldry subreddit, there my username is " [:em "or#5915"]]]])
